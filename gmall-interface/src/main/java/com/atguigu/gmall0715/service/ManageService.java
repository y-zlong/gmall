package com.atguigu.gmall0715.service;

import com.atguigu.gmall0715.bean.*;

import java.util.List;

public interface ManageService {
    //查询所有一级分类
    List<BaseCatalog1> getCatalog1();

    //根据一级分类查询二级分类
    List<BaseCatalog2> getCatalog2(String Catalog1Id);

    //根据二级分类查询三级分类
    List<BaseCatalog3> getCatalog3(String Catalog2Id);

    //根据三级分类查询Id平台属性列表
    List<BaseAttrInfo> getAttrList(String Catalog3Id);

    //平台属性值，平台属性大保存
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    BaseAttrInfo getAttrInfo(String attrId);

    //根据三级分类ID查询商品列表catalog3Id
    List<SpuInfo> getSpuInfoList(SpuInfo spuInfo);

    // 查询基本销售属性表
    List<BaseSaleAttr> getBaseSaleAttrList();

    //保存事件
    void saveSpuInfo(SpuInfo spuInfo);

    //根据spuid查询spuimg
    List<SpuImage> getSpuImgList(String spuId);

    //根据spuId查询销售属性列表集合
    List<SpuSaleAttr> getSpuSaleAttrList(String spuId);


    void saveSkuInfo(SkuInfo skuInfo);

    //根据skuId查询skuinfo信息
    SkuInfo getSkuInfo(String skuId);

    //查询销售属性 以及销售属性值
    List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(SkuInfo skuInfo);

    //根据spuid查询skusaleAttrValue
    List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId);
}
