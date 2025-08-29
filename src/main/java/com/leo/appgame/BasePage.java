package com.leo.appgame;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;

public abstract class BasePage extends WebPage {
    public BasePage() {
        // ✅ IDs corrigidos para bater com o HTML
        add(new BookmarkablePageLink<>("linkHome", HomePage.class));
        add(new BookmarkablePageLink<>("linkNovoJogo", NovoJogoPage.class));

        // Feedback global
        FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true); // necessário para o JS
        add(feedback);
    }
}
