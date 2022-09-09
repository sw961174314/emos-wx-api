package com.sw961174314.emos.wx.db.dao;

import com.sw961174314.emos.wx.db.pojo.TbFaceModel;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TbFaceModelDao {

    // 查找人脸模型
    public String searchFaceModel(int userId);

    // 新建人脸模型
    public void insert(TbFaceModel faceModelEntity);

    // 删除人脸模型
    public int deleteFaceModel(int userId);
}

