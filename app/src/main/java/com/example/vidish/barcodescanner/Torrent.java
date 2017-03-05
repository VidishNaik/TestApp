package com.example.vidish.barcodescanner;

/**
 * Created by Vidish on 05-03-2017.
 */

public class Torrent {
    private String url;
    private String hash;
    private String quality;
    private String seeds;
    private String peers;
    private String size;

    public Torrent(String url, String hash, String quality, String seeds, String peers, String size) {
        this.url = url;
        this.hash = hash;
        this.quality = quality;
        this.seeds = seeds;
        this.peers = peers;
        this.size = size;
    }
}
