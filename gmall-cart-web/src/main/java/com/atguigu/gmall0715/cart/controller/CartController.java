package com.atguigu.gmall0715.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0715.bean.CartInfo;
import com.atguigu.gmall0715.bean.SkuInfo;
import com.atguigu.gmall0715.config.LoginRequire;
import com.atguigu.gmall0715.service.CartService;

import com.atguigu.gmall0715.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
public class CartController {


    @Reference
    private CartService cartService;

    @Autowired
    private CartCookieHandler cartCookieHandler;

    @Reference
    private ManageService manageService;


    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response){
       String skuNum = request.getParameter("skuNum");
       String skuId = request.getParameter("skuId");
       String userId = (String) request.getAttribute("userId");
       if(userId!=null){
           cartService.addToCart(skuId,userId,Integer.parseInt(skuNum));
       }else{
           cartCookieHandler.addToCart(request,response,skuId,userId,Integer.parseInt(skuNum));
       }

        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum",skuNum);
        return "success";
   }

   @RequestMapping("cartList")
   @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request,HttpServletResponse response){
       String userId = (String) request.getAttribute("userId");
       if (userId != null){
           List<CartInfo> cartInfoList = null;
           //取得cookie中的数据
           List<CartInfo> cartListCK = cartCookieHandler.getCartList(request);
            if(cartListCK != null && cartListCK.size()>0){
                //合并 useriID
                cartInfoList = cartService.mergeToCartList(cartListCK,userId);
                //合并完成之后将cookie数据删除
                cartCookieHandler.deleteCartCookie(request,response);
            }else {
                 cartInfoList = cartService.getCartList(userId);
            }

           request.setAttribute("cartInfoList",cartInfoList);
       }else{
           //去cookie中取得数据
          List<CartInfo> cartInfoList = cartCookieHandler.getCartList(request);
          request.setAttribute("cartInfoList",cartInfoList);
       }
       return "cartList";
   }


    @RequestMapping("checkCart")
    @ResponseBody
    @LoginRequire(autoRedirect = false)
    public void checkCart(HttpServletRequest request,HttpServletResponse response){
        //获取前台传来的参数
        String isChecked = request.getParameter("isChecked");
        String skuId = request.getParameter("skuId");
        //在作用域中获取userid
        String userId = (String) request.getAttribute("userId");
        //判断用户是否登录
        if(userId != null){
            //修改redis
            cartService.checkCart(skuId,isChecked,userId);
        }else {
            //修改cookie==给cookie重新赋值
            cartCookieHandler.checkCart(request,response,skuId,isChecked);
        }

    }

    @RequestMapping("toTrade")
    @LoginRequire(autoRedirect = true)
    public String toTrade(HttpServletRequest request,HttpServletResponse response){
        List<CartInfo> cartListCK = cartCookieHandler.getCartList(request);
        //合并过程中需要userID
        String userId = (String) request.getAttribute("userId");

        if (cartListCK !=null && cartListCK.size()>0){
            //合并勾选状态
            cartService.mergeToCartList(cartListCK,userId);
            //将勾选中的数据删除
            cartCookieHandler.deleteCartCookie(request,response);
        }

        //返回订单去结算的购物车
        return "redirect://order.gmall.com/trade";
    }
}
