package nhom2.NguyenTanSang.constants;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Role {
    ADMIN(1L),
    USER(2L);
    
    public final Long value;
}
