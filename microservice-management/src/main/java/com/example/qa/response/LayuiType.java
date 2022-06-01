package com.example.qa.response;

import lombok.Data;

/**
 * @Author: lerry_li
 * @CreateDate: 2021/12/23
 */
@Data
public class LayuiType {
    private Integer code;
    private String msg;
    private Integer count;
    private Object data;

    public static LayuiType create(Integer code, String msg, Integer count, Object data) {
        LayuiType layuiType = new LayuiType();
        layuiType.setCode(code);
        layuiType.setCount(count);
        layuiType.setMsg(msg);
        layuiType.setData(data);
        return layuiType;
    }
}
