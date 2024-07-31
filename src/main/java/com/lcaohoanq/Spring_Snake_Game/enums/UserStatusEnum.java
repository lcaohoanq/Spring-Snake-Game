package com.lcaohoanq.Spring_Snake_Game.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatusEnum {

    UNVERIFIED(0),
    VERIFIED(1),
    BANNED(2);

    private final int status;
}
