package top.cocoawork.monitor.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Mapper;
import top.cocoawork.monitor.dao.entity.UserRole;

@CacheNamespace
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {



}
