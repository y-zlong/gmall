package com.atguigu.gmall0715.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0715.bean.CartInfo;
import com.atguigu.gmall0715.bean.SkuInfo;
import com.atguigu.gmall0715.cart.constant.CartConst;
import com.atguigu.gmall0715.cart.mapper.CartInfoMapper;
import com.atguigu.gmall0715.config.RedisUtil;
import com.atguigu.gmall0715.service.CartService;
import com.atguigu.gmall0715.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.*;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Reference
    private ManageService manageService;

    @Autowired
    private RedisUtil redisUtil;


    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        //添加购物车的业务
        //查看购物车中是否有该商品
        CartInfo cartInfo = new CartInfo();
        cartInfo.setSkuId(skuId);
        cartInfo.setUserId(userId);
        //根据skuId 和 userId 查询购物车是否有该商品
        CartInfo cartInfoExist  = cartInfoMapper.selectOne(cartInfo);
        if(cartInfoExist != null){
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            //给实时购物车商品附上值（价格）
            cartInfoExist.setSkuPrice(cartInfoExist.getCartPrice());
            //跟新数据库
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);
            //跟新缓存到redis

        }else{
            //新增商品数据来源于skuinfo
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            //根据skuid获得skuinfo对象
            CartInfo cartInfo1 = new CartInfo();
            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setSkuNum(skuNum);
            cartInfo1.setUserId(userId);
            cartInfo1.setSkuId(skuId);
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setCartPrice(skuInfo.getPrice());

            cartInfoMapper.insertSelective(cartInfo1);
            //从入redis
            cartInfoExist = cartInfo1;
        }

        //存数据到缓存redis

        Jedis jedis = redisUtil.getJedis();
        //定义key
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;

        jedis.hset(cartKey,skuId, JSON.toJSONString(cartInfoExist));
        String userKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USERINFOKEY_SUFFIX;
        //设置过期时间
        Long ttl = jedis.ttl(userKey);
        jedis.expire(cartKey,ttl.intValue());
        jedis.close();
    }

    @Override
    public List<CartInfo> getCartList(String userId) {
        //从redis取得数据
        Jedis jedis = redisUtil.getJedis();
        //定义key
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        //根据key取得购物车数据
        List<String> cartJsons  = jedis.hvals(userCartKey);
        if (cartJsons!=null&&cartJsons.size()>0){
            List<CartInfo> cartInfoList = new ArrayList<>();
            for (String cartJson : cartJsons) {
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                cartInfoList.add(cartInfo);
            }
            // 排序
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return Long.compare(Long.parseLong(o2.getId()),Long.parseLong(o1.getId()));
                }
            });
            return cartInfoList;
        }else{
            // 从数据库中查询，其中cart_price 可能是旧值，所以需要关联sku_info 表信息。
            List<CartInfo> cartInfoList = loadCartCache(userId);
            return  cartInfoList;
        }

    }

    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId) {
        // 能够得到最新的商品价格的cartInfoList
        List<CartInfo> cartInfoListDB = cartInfoMapper.selectCartListWithCurPrice(userId);
        // CK ,redis 判断条件 是否有相同的skuId
        // 将cookie 中的数据都添加到数据库！
        for (CartInfo cartInfoCK : cartListCK) {
            boolean isMatch =false;
            for (CartInfo cartInfoDB : cartInfoListDB) {
                // 匹配上
                if (cartInfoCK.getSkuId().equals(cartInfoDB.getSkuId())){
                    // 数量相加
                    cartInfoDB.setSkuNum(cartInfoDB.getSkuNum()+cartInfoCK.getSkuNum());
                    // 更新数据库
                    cartInfoMapper.updateByPrimaryKeySelective(cartInfoDB);
                    isMatch = true;
                }
            }
            // 没有匹配上！
            if (!isMatch){
                // 直接插入数据库！将userId 存入到数据库
                cartInfoCK.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfoCK);
            }
        }
        // 放入redis
        List<CartInfo> cartInfoList = loadCartCache(userId);

        //合并勾选状态下的购物车选项，合并条件是skuId isChecked=1
        for (CartInfo cartInfoDB : cartInfoList) {
            for (CartInfo cartInfoCK : cartListCK) {
                //skuId
                if(cartInfoDB.getSkuId().equals(cartInfoCK.getSkuId())){
                    if("1".equals(cartInfoCK.getIsChecked())){
                        //被勾选的商品合并
                        cartInfoDB.setSkuNum(cartInfoDB.getSkuNum()+cartInfoCK.getSkuNum());
                        //保存勾选状态下的redis
                        checkCart(cartInfoDB.getSkuId(),cartInfoCK.getIsChecked(),userId);
                    }
                }
            }
        }
        // 返回最终的合并数据！
        return cartInfoList;
    }

    @Override
    public void checkCart(String skuId, String isChecked, String userId) {
        //获取redis
        Jedis jedis = redisUtil.getJedis();
        //获取数据的key user :userId:cart
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        //获取key对应的值
        String cartJson = jedis.hget(userCartKey, skuId);
        //将字符串装换对象
        CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
        //给cartInfo赋值
        cartInfo.setIsChecked(isChecked);
        //将赋值好的对象放入redis
        jedis.hset(userCartKey,skuId,JSON.toJSONString(cartInfo));

        //新增一个可以专门存储被勾选中的商品
        String userCheckKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CHECKED_KEY_SUFFIX;
        if("1".equals(isChecked)){
            //user:userID:checkkey
            jedis.hset(userCheckKey,skuId,JSON.toJSONString(cartInfo));
        }else {
            //删除key
            jedis.hdel(userCheckKey,skuId);
        }
        jedis.close();
    }


    public List<CartInfo> loadCartCache(String userId) {
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        if (cartInfoList==null && cartInfoList.size()==0){
            return null;
        }
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedis();
        Map<String,String> map = new HashMap<>(cartInfoList.size());
        for (CartInfo cartInfo : cartInfoList) {
            String cartJson = JSON.toJSONString(cartInfo);
            // key 都是同一个，值会产生重复覆盖！
            map.put(cartInfo.getSkuId(),cartJson);
        }
        // 将java list - redis hash
        jedis.hmset(userCartKey,map);
        jedis.close();
        return  cartInfoList;

    }
}






















