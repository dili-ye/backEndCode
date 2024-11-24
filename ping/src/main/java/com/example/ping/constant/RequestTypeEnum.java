package com.example.ping.constant;

/**
 *
 * @author admin
 */
public enum RequestTypeEnum {

    OK(200,"Request sent & Pong Responded: "),
    TOO_MANY_REQUEST(429,"Request sent & Pong throttled it."),
    REQUEST_LIMIT(-2,"Request not send as being “rate limited”."),
    UNKNOWN_ERROR(-3,"UNKNOWN_ERROR: ");

    private Integer code;
    private String desc;

    RequestTypeEnum(Integer code , String desc){
        this.code = code;
        this.desc = desc;
    }


    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
