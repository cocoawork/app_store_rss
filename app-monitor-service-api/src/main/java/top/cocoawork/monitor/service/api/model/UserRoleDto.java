package top.cocoawork.monitor.service.api.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class UserRoleDto extends BaseModelDto implements Serializable {

    private Long userId;
    private String userRole;

}