package lk.tech.moonauth.auth.mapper;

import lk.tech.moonauth.auth.dto.UserResponse;
import lk.tech.moonauth.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthMapper {
    UserResponse toUserResponse(User user);
}
