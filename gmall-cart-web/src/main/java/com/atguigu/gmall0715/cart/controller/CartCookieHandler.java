package com.atguigu.gmall0715.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0715.bean.CartInfo;
import com.atguigu.gmall0715.bean.SkuInfo;
import com.atguigu.gmall0715.config.CookieUtil;
import com.atguigu.gmall0715.service.ManageService;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
    操作cookie中的数据

 */
@Component
public class CartCookieHandler {

    // 定义购物车名称
    private String COOKIECARTNAME = "CART";
    // 设置cookie 过期时间
    private int COOKIE_CART_MAXAGE=7*24*3600;
    @Reference
    private ManageService manageService;

    // 未登录的时候，添加到购物车
    public void addToCart(HttpServletRequest request, HttpServletResponse response, String skuId, String userId, Integer skuNum){
        //判断cookie中是否有购物车 有可能有中文，所有要进行序列化
        String cartJson = CookieUtil.getCookieValue(request, COOKIECARTNAME, true);
        List<CartInfo> cartInfoList = new ArrayList<>();
        boolean ifExist=false;
        if (cartJson!=null){
            cartInfoList = JSON.parseArray(cartJson, CartInfo.class);
            for (CartInfo cartInfo : cartInfoList) {
                if (cartInfo.getSkuId().equals(skuId)){
                    cartInfo.setSkuNum(cartInfo.getSkuNum()+skuNum);
                    // 价格设置
                    cartInfo.setSkuPrice(cartInfo.getCartPrice());
                    ifExist=true;
                    break;
                }
            }
        }
        // //购物车里没有对应的商品 或者 没有购物车
        if (!ifExist){
            //把商品信息取出来，新增到购物车
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo=new CartInfo();

            cartInfo.setSkuId(skuId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());

            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(skuNum);
            cartInfoList.add(cartInfo);
        }
        // 把购物车写入cookie
        String newCartJson = JSON.toJSONString(cartInfoList);
        CookieUtil.setCookie(request,response,COOKIECARTNAME,newCartJson,COOKIE_CART_MAXAGE,true);
    }
    //取得购物车数据
    public List<CartInfo> getCartList(HttpServletRequest request) {
        //取得集合数据
        String cartJson = CookieUtil.getCookieValue(request, COOKIECARTNAME, true);
        //转换
        List<CartInfo> cartInfoList = JSON.parseArray(cartJson, CartInfo.class);
        return  cartInfoList;
    }
    //删除cookie中的数据
    public void deleteCartCookie(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.deleteCookie(request,response,COOKIECARTNAME);

    }

    /**
     * 就该cookie中的商品状态
     * @param request
     * @param response
     * @param skuId
     * @param isChecked
     */
    public void checkCart(HttpServletRequest request, HttpServletResponse response, String skuId, String isChecked) {
        //先获取cookie中的购物车集合
        List<CartInfo> cartList = getCartList(request);
        //循环判断购物车里的商品是否与添加的商品一致
        for (CartInfo cartInfo : cartList) {
            if(cartInfo.getSkuId().equals(skuId)){
                cartInfo.setIsChecked(isChecked);
            }
        }
        //将修改之后的商品放入cookie中
        CookieUtil.setCookie(request,response,COOKIECARTNAME,JSON.toJSONString(cartList),COOKIE_CART_MAXAGE,true);
    }
}
