package com.lcaohoanq.Spring_Snake_Game.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public abstract class AbstractResponse {

    private String message;
    private String status;

}