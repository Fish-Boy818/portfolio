package com.untitled.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

public class ClubCreateRequest {
    @NotBlank
    private String name;

    @Size(max = 128)
    private String location;

    @Size(max = 255)
    private String address;

    @Size(max = 32)
    private String phone;

    @Size(max = 64)
    private String openTime;

    @Size(max = 255)
    private String douyinUrl;

    private List<String> tags;
    private String cover;
    private String licenseImage;
    private List<String> gallery;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getOpenTime() {
        return openTime;
    }

    public void setOpenTime(String openTime) {
        this.openTime = openTime;
    }

    public String getDouyinUrl() {
        return douyinUrl;
    }

    public void setDouyinUrl(String douyinUrl) {
        this.douyinUrl = douyinUrl;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getLicenseImage() {
        return licenseImage;
    }

    public void setLicenseImage(String licenseImage) {
        this.licenseImage = licenseImage;
    }

    public List<String> getGallery() {
        return gallery;
    }

    public void setGallery(List<String> gallery) {
        this.gallery = gallery;
    }
}
