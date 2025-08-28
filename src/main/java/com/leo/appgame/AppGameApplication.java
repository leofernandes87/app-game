package com.leo.appgame;

import org.apache.wicket.Page;
import org.apache.wicket.protocol.http.WebApplication;

public class AppGameApplication extends WebApplication {
    @Override
    public Class<? extends Page> getHomePage() {
        return HomePage.class;
    }

    @Override
    public void init() {
        super.init();
    }
}
