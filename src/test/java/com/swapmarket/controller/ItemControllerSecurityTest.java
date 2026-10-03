package com.swapmarket.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.swapmarket.config.SecurityConfig;
import com.swapmarket.mapper.ResponseMapper;
import com.swapmarket.security.JwtAuthFilter;
import com.swapmarket.service.ItemService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ItemController.class)
@Import(SecurityConfig.class)
class ItemControllerSecurityTest {

    // TODO: replace with valid JSON for your ItemRequest.
    // It must pass validation, otherwise you get 400 before the role check runs.
      private static final String VALID_ITEM_JSON = "{\"title\":\"Denim Jacket\",\"description\":\"Barely worn\",\"category\":\"Jackets\",\"size\":\"M\",\"condition\":\"Good\"}";
    @Autowired private MockMvc mvc;

    @MockBean private ItemService itemService;
    @MockBean private ResponseMapper mapper;
    @MockBean private JwtAuthFilter jwtAuthFilter;
    @MockBean private UserDetailsService userDetailsService;

    @BeforeEach
    void letRequestsPassThroughMockedJwtFilter() throws Exception {
        doAnswer(inv -> {
            ServletRequest req = inv.getArgument(0);
            ServletResponse res = inv.getArgument(1);
            FilterChain chain = inv.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    @Test
    void anonymousUser_isRejected() throws Exception {
        mvc.perform(get("/items"))
           .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "USER")
    void user_canBrowseItems() throws Exception {
        mvc.perform(get("/items"))
           .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void user_cannotCreateItem() throws Exception {
        mvc.perform(post("/items")
                .contentType("application/json")
                .content(VALID_ITEM_JSON))
           .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SELLER")
    void seller_canCreateItem() throws Exception {
        mvc.perform(post("/items")
                .contentType("application/json")
                .content(VALID_ITEM_JSON))
           .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_canCreateItem() throws Exception {
        mvc.perform(post("/items")
                .contentType("application/json")
                .content(VALID_ITEM_JSON))
           .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void user_cannotAccessAdminArea() throws Exception {
        mvc.perform(get("/admin/anything"))
           .andExpect(status().isForbidden());
    }
}