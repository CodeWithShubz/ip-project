package com.travelaggregator.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the frontend single-page application.
 * All non-API routes return index.html.
 */
@Controller
public class FrontendController {

    @GetMapping(value = {"/", "/index", "/search", "/booking", "/my-bookings"})
    public String index() {
        return "forward:/index.html";
    }
}
