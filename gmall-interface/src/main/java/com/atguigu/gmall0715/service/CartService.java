package com.atguigu.gmall0715.service;

import com.atguigu.gmall0715.bean.CartInfo;

import java.util.List;

public interface CartService {
    /**
     *
     * @param skuId 商品ID
     * @param userId 用户ID
     * @param skuNum 购买商品数量
     */
    public  void  addToCart(String skuId,String userId,Integer skuNum);

    //根据用户Id创建
    List<CartInfo> getCartList(String userId);

    /**
     * 合并购物车
     * @param cartListCK
     * @param userId
     * @return
     */
    List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId) ;

    /**
     * 更改商品的选中状态
     * @param skuId
     * @param isChecked
     * @param userId
     */
    void checkCart(String skuId, String isChecked, String userId);


}


