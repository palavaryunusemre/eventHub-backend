package com.eventhub.config;

public class RestApis {
    public static final String MAIN = "api";

    public static final String AUTH = MAIN + "/auth";
    public static final String SIGN_IN = "/signIn";
    public static final String LOG_IN = "/logIn";
    public static final String LOG_OUT = "/logout";
    public static final String REFRESH = "/refresh";
    public static final String DELETE_USER = "/delete";
    public static final String GET_USERS = "/users";

    public static final String INSTITUTION = MAIN + "/institutions";
    public static final String CATEGORY = MAIN + "/categories";
    public static final String EVENT = MAIN + "/events";
    public static final String ADMIN_EVENT = MAIN + "/admin/events";
    public static final String REGISTRATION = MAIN + "/registrations";
    public static final String CHECKIN = MAIN + "/checkins";
    public static final String CERTIFICATE = MAIN + "/certificates";
    public static final String VERIFY = MAIN + "/verify";
    public static final String AUDIT = MAIN + "/audit";
}
