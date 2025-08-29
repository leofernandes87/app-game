package com.leo.appgame;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;

public class HomePage extends WebPage {
    public HomePage() {
        add(new Label("mensagem","Bem vindo ao APP-GAME"));
        add(new BookmarkablePageLink<Void>("linkNovoJogo", com.leo.appgame.NovoJogoPage.class));

    }
}
