package com.atguigu.gmall0715.service;

import com.atguigu.gmall0715.bean.SkuLsInfo;
import com.atguigu.gmall0715.bean.SkuLsParams;
import com.atguigu.gmall0715.bean.SkuLsResult;

public interface ListService {
    public void saveSkuInfo(SkuLsInfo skuLsInfo);

    //通过用户筛选条件来查询数据
    public SkuLsResult search(SkuLsParams skuLsParams);
}
