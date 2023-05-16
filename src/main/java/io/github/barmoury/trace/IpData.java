package io.github.barmoury.trace;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IpData {

    Isp isp;
    Location location;

}
