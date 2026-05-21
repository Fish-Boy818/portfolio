package com.untitled.service;

import com.untitled.dto.BannerCreateRequest;
import com.untitled.dto.BannerResponse;
import com.untitled.dto.BannerUpdateRequest;
import com.untitled.mapper.BannerMapper;
import com.untitled.model.Banner;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BannerService {
    private final BannerMapper bannerMapper;

    public BannerService(BannerMapper bannerMapper) {
        this.bannerMapper = bannerMapper;
    }

    public List<BannerResponse> listAll() {
        return bannerMapper.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 作用：
     * 查询首页展示所需的启用轮播图数据。
     * 方法：
     * 先读取状态为启用的轮播记录，再统一转换为前端响应对象，
     * 最后返回首页轮播列表。
     */
    public List<BannerResponse> listActive() {
        // 作用：查询首页轮播图列表；方法：读取启用轮播并转换为前端响应对象
        return bannerMapper.findActive().stream()
                // 作用：查询启用轮播记录；方法：调用 Mapper 从数据库读取 active 状态数据
                .map(this::toResponse)
                // 作用：转换轮播对象；方法：把实体对象映射为 BannerResponse
                .collect(Collectors.toList());
        // 作用：组装返回列表；方法：把流式结果收集为 List
    }

    public Optional<BannerResponse> get(long id) {
        Banner banner = bannerMapper.findById(id);
        if (banner == null) {
            return Optional.empty();
        }
        return Optional.of(toResponse(banner));
    }

    public BannerResponse create(BannerCreateRequest request) {
        Banner banner = new Banner();
        banner.setTitle(request.getTitle() != null ? request.getTitle().trim() : "");
        banner.setSubtitle(request.getSubtitle());
        banner.setImageUrl(request.getImageUrl());
        banner.setStatus(request.getStatus() != null ? request.getStatus() : "active");
        banner.setSort(request.getSort() != null ? request.getSort() : 0);
        bannerMapper.insert(banner);
        return toResponse(bannerMapper.findById(banner.getId()));
    }

    public Optional<BannerResponse> update(long id, BannerUpdateRequest request) {
        Banner banner = bannerMapper.findById(id);
        if (banner == null) {
            return Optional.empty();
        }
        banner.setTitle(request.getTitle() != null ? request.getTitle().trim() : "");
        banner.setSubtitle(request.getSubtitle());
        banner.setImageUrl(request.getImageUrl());
        banner.setStatus(request.getStatus() != null ? request.getStatus() : banner.getStatus());
        banner.setSort(request.getSort() != null ? request.getSort() : banner.getSort());
        bannerMapper.update(banner);
        return Optional.of(toResponse(bannerMapper.findById(id)));
    }

    public boolean delete(long id) {
        return bannerMapper.delete(id) > 0;
    }

    private BannerResponse toResponse(Banner banner) {
        BannerResponse response = new BannerResponse();
        response.setId(banner.getId());
        response.setTitle(banner.getTitle());
        response.setSubtitle(banner.getSubtitle());
        response.setImageUrl(banner.getImageUrl());
        response.setStatus(banner.getStatus());
        response.setSort(banner.getSort());
        return response;
    }
}
