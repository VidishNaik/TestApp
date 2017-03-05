package com.example.vidish.barcodescanner;

/**
 * Created by Vidish on 05-03-2017.
 */

public class Movies {
    private String id;
    private String imdb;
    private String title;
    private String slug;
    private String rating;
    private String runtime;
    private String[] genres;
    private String description;
    private String youtube;
    private String cover;
    private Torrent torrent720;
    private Torrent torrent1080;
    private Torrent torrent3d;

    public Movies(String id,String imdb, String title, String slug, String rating, String runtime, String[] genres, String description, String youtube, String cover, Torrent torrent720, Torrent torrent1080, Torrent torrent3d) {
        this.id = id;
        this.imdb = imdb;
        this.title = title;
        this.slug = slug;
        this.rating = rating;
        this.runtime = runtime;
        this.genres = genres;
        this.description = description;
        this.youtube = youtube;
        this.cover = cover;
        this.torrent720 = torrent720;
        this.torrent1080 = torrent1080;
        this.torrent3d = torrent3d;
    }

    public String getId() {
        return id;
    }
}
