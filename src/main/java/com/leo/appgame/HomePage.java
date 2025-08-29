package com.leo.appgame;

import org.apache.wicket.markup.html.basic.Label;

public class HomePage extends BasePage {
    public HomePage() {
        add(new Label("mensagem","Bem vindo ao APP-GAME"));;
    }
}
