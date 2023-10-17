package com.quanly.hoatdongcongdong.service;

import com.quanly.hoatdongcongdong.entity.GioTichLuy;
import com.quanly.hoatdongcongdong.repository.GioTichLuyRepository;
import com.quanly.hoatdongcongdong.repository.HoatDongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GioTichLuyService {

    private final GioTichLuyRepository gioTichLuyRepository;

    @Autowired
    public GioTichLuyService(GioTichLuyRepository gioTichLuyRepository) {
        this.gioTichLuyRepository = gioTichLuyRepository;
    }

    public GioTichLuy findByGiangVien_MaTaiKhoan(Long maTk) {
        return gioTichLuyRepository.findByGiangVien_MaTaiKhoan(maTk);
    }

    public List<GioTichLuy> findByNam(String nam) {
        return gioTichLuyRepository.findByNam(nam);
    }

    public GioTichLuy findByGiangVien_MaTaiKhoanAndNam(Long nguoiDungId, String nam) {
        return gioTichLuyRepository.findByGiangVien_MaTaiKhoanAndNam(nguoiDungId, nam);
    }

    public List<String> findDistinctNamByGiangVien(Long maTk) {
        return gioTichLuyRepository.findDistinctNamByGiangVien(maTk);
    }

    public GioTichLuy findByNamAndGiangVien_MaTaiKhoan(String nam, Long maTk) {
        return gioTichLuyRepository.findByNamAndGiangVien_MaTaiKhoan(nam, maTk);
    }
}


