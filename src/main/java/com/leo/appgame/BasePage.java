package com.leo.appgame;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;

public abstract class BasePage extends WebPage {
    public BasePage() {
        add(new BookmarkablePageLink<>("linkHome", HomePage.class));
        add(new BookmarkablePageLink<>("linkNovoJogo", NovoJogoPage.class));
        add(new BookmarkablePageLink<>("linkTodosJogos", TodosJogosPage.class));

        // Feedback global
        FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);
    }
}
