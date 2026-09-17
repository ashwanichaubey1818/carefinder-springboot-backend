package com.carefinder.backend.personal;

import com.carefinder.backend.common.ApiMessage;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Personal Hospital Tools")
public class PersonalController {

    private final FavoriteService favoriteService;
    private final RecentlyViewedService recentlyViewedService;

    public PersonalController(FavoriteService favoriteService, RecentlyViewedService recentlyViewedService) {
        this.favoriteService = favoriteService;
        this.recentlyViewedService = recentlyViewedService;
    }

    @GetMapping("/favorites")
    List<PersonalDtos.FavoriteResponse> favorites() {
        return favoriteService.list();
    }

    @PostMapping("/favorites/{hospitalId}")
    @ResponseStatus(HttpStatus.CREATED)
    PersonalDtos.FavoriteResponse addFavorite(@PathVariable Long hospitalId) {
        return favoriteService.add(hospitalId);
    }

    @DeleteMapping("/favorites/{hospitalId}")
    ApiMessage removeFavorite(@PathVariable Long hospitalId) {
        favoriteService.remove(hospitalId);
        return new ApiMessage("Hospital removed from favorites.");
    }

    @DeleteMapping("/favorites")
    ApiMessage clearFavorites() {
        favoriteService.clear();
        return new ApiMessage("Favorites cleared.");
    }

    @GetMapping("/recently-viewed")
    List<PersonalDtos.RecentResponse> recentlyViewed() {
        return recentlyViewedService.list();
    }

    @PostMapping("/recently-viewed/{hospitalId}")
    PersonalDtos.RecentResponse recordRecentlyViewed(@PathVariable Long hospitalId) {
        return recentlyViewedService.record(hospitalId);
    }

    @DeleteMapping("/recently-viewed/{hospitalId}")
    ApiMessage removeRecentlyViewed(@PathVariable Long hospitalId) {
        recentlyViewedService.remove(hospitalId);
        return new ApiMessage("Hospital removed from recently viewed.");
    }

    @DeleteMapping("/recently-viewed")
    ApiMessage clearRecentlyViewed() {
        recentlyViewedService.clear();
        return new ApiMessage("Recently viewed history cleared.");
    }
}
