package com.quanly.hoatdongcongdong.service;

import com.quanly.hoatdongcongdong.entity.*;
import com.quanly.hoatdongcongdong.payload.response.HoatDongDTO;
import com.quanly.hoatdongcongdong.payload.response.HoatDongResponse;
import com.quanly.hoatdongcongdong.payload.response.HoatDongTongHopResponse;
import com.quanly.hoatdongcongdong.payload.response.MessageResponse;
import com.quanly.hoatdongcongdong.repository.*;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class HoatDongService {
    @Autowired
    private GioTichLuyRepository gioTichLuyRepository;
    @Autowired
    private ChucDanhRepository chucDanhRepository;
    @Autowired
    private FilesStorageService storageService;
    private final HoatDongRepository hoatDongRepository;
    private final LoaiHoatDongRepository loaiHoatDongRepository;
    private final GiangVienRepository giangVienRepository;
    private final DangKyHoatDongRepository dangKyHoatDongRepository;
    private final HoatDongNgoaiTruongRepository hoatDongNgoaiTruongRepository;
    @Autowired
    public HoatDongService(
            HoatDongRepository hoatDongRepository,
            LoaiHoatDongRepository loaiHoatDongRepository,
            GiangVienRepository giangVienRepository,
            DangKyHoatDongRepository dangKyHoatDongRepository,
            HoatDongNgoaiTruongRepository hoatDongNgoaiTruongRepository
    ) {
        this.hoatDongRepository = hoatDongRepository;
        this.loaiHoatDongRepository = loaiHoatDongRepository;
        this.giangVienRepository = giangVienRepository;
        this.dangKyHoatDongRepository = dangKyHoatDongRepository;
        this.hoatDongNgoaiTruongRepository =hoatDongNgoaiTruongRepository;
    }

    public List<Integer> findYears() {
        return hoatDongRepository.findYears();
    }

    public Long countByTrangThaiHoatDong(HoatDong.TrangThaiHoatDong trangThaiHoatDong) {
        return hoatDongRepository.countByTrangThaiHoatDong(trangThaiHoatDong);
    }

    public Optional<HoatDong> findById(Long maHoatDong) {
        return hoatDongRepository.findById(maHoatDong);
    }

    public Page<HoatDong> getAllHoatDong(
            int page,
            int size,
            String sortBy,
            String sortDir,
            String searchTerm,
            String type,
            HoatDong.TrangThaiHoatDong status,
            String startTime,
            String endTime
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable paging = PageRequest.of(page, size, sort);

        Specification<HoatDong> spec = Specification.where(null);

        if (!searchTerm.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.or(
                            criteriaBuilder.like(root.get("tenHoatDong"), "%" + searchTerm + "%"),
                            criteriaBuilder.like(root.get("moTa"), "%" + searchTerm + "%")
                    ));
        }
        if (status != null) {
            spec = spec.and((root, criteriaQuery, criteriaBuilder)
                    -> criteriaBuilder.equal(root.get("trangThaiHoatDong"), status));
        }
        if (!type.isEmpty()) {
            spec = spec.and((root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("loaiHoatDong").get("tenLoaiHoatDong"), type));
        }

        if (startTime != null) {
            LocalDateTime startTimes = LocalDate.parse(startTime).atStartOfDay();
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("thoiGianBatDau"), startTimes));
        }

        if (endTime != null) {
            LocalDate date = LocalDate.parse(endTime);
            LocalDateTime endTimes = date.atTime(23, 59, 59);
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("thoiGianKetThuc"), endTimes));
        }

        return hoatDongRepository.findAll(spec, paging);
    }
    @Transactional
    public HoatDong addHoatDong(HoatDongResponse hoatDongResponse) {

        // Kiểm tra nếu loại hoạt động không tồn tại
        Optional<LoaiHoatDong> loaiHoatDong = loaiHoatDongRepository.findById(hoatDongResponse.getMaLoaiHoatDong());
        if (loaiHoatDong.isEmpty()) {
            new ResponseEntity<>(new MessageResponse("hoatdong-notfound"), HttpStatus.NOT_FOUND);
            return null;
        }
        Optional<HoatDong> existingHoatDong = hoatDongRepository.findByTenHoatDong(hoatDongResponse.getTenHoatDong());
        if (existingHoatDong.isPresent()) {
            throw new EntityExistsException("hoatdong-exist");
        }
        HoatDong hoatDong = new HoatDong();
        hoatDong.setTenHoatDong(hoatDongResponse.getTenHoatDong());
        hoatDong.setMoTa(hoatDongResponse.getMoTa());
        hoatDong.setDiaDiem(hoatDongResponse.getDiaDiem());
        hoatDong.setGioTichLuyThamGia(hoatDongResponse.getGioTichLuyThamGia());
        hoatDong.setGioTichLuyToChuc(hoatDongResponse.getGioTichLuyToChuc());
        hoatDong.setThoiGianBatDau(hoatDongResponse.getThoiGianBatDau());
        hoatDong.setThoiGianKetThuc(hoatDongResponse.getThoiGianKetThuc());
        hoatDong.setLoaiHoatDong(loaiHoatDong.get());

        List<GiangVien> giangVienToChucs = giangVienRepository.findByTaiKhoan_TenDangNhapIn(hoatDongResponse.getGiangVienToChucs());
        hoatDong.setGiangVienToChucs(giangVienToChucs);
        hoatDong.setTenQuyetDinh(hoatDongResponse.getTenQuyetDinh());
        hoatDong.setSoQuyetDinh(hoatDongResponse.getSoQuyetDinh());
        hoatDong.setCapToChuc(hoatDongResponse.getCapToChuc());

        hoatDong.setNguoiKyQuyetDinh(hoatDongResponse.getNguoiKyQuyetDinh());

        hoatDongRepository.save(hoatDong);
        // cập nhật giờ tích lu tổ chuc cho giảng viên
        List<GiangVien> dsGiangVienToChucs = hoatDong.getGiangVienToChucs();
        String namHoc = String.valueOf(hoatDong.getThoiGianBatDau().getYear());
        for (GiangVien giangVienToChuc : dsGiangVienToChucs) {
            int gioTichLuyToChuc = hoatDong.getGioTichLuyToChuc();
            GioTichLuy gioTichLuyToChucEntity = gioTichLuyRepository.findByGiangVien_MaTaiKhoanAndNam(giangVienToChuc.getMaTaiKhoan(), namHoc);

            if (gioTichLuyToChucEntity == null) {
                gioTichLuyToChucEntity = new GioTichLuy();
                gioTichLuyToChucEntity.setGiangVien(giangVienToChuc);
                gioTichLuyToChucEntity.setTongSoGio(gioTichLuyToChuc);
                gioTichLuyToChucEntity.setNam(namHoc);
            } else {
                gioTichLuyToChucEntity.setTongSoGio(gioTichLuyToChucEntity.getTongSoGio() + gioTichLuyToChuc);
            }
            gioTichLuyRepository.save(gioTichLuyToChucEntity);
        }
        return hoatDong;
    }

    public HoatDong updateHoatDong(Long maHoatDong, HoatDongResponse hoatDongResponse) {

        // Kiểm tra hoạt động có tồn tại không
        Optional<HoatDong> existingHoatDong = hoatDongRepository.findById(maHoatDong);
        if (existingHoatDong.isEmpty()) {
            new ResponseEntity<>(new MessageResponse("hoatdong-notfound"), HttpStatus.NOT_FOUND);
            return null;
        }

        // Kiểm tra nếu loại hoạt động không tồn tại
        Optional<LoaiHoatDong> loaiHoatDong = loaiHoatDongRepository.findById(hoatDongResponse.getMaLoaiHoatDong());
        if (loaiHoatDong.isEmpty()) {
            new ResponseEntity<>(new MessageResponse("loaihoatdong-notfound"), HttpStatus.OK);
            return null;
        }
        Optional<HoatDong> existingHoatDong1 = hoatDongRepository.findByTenHoatDong(hoatDongResponse.getTenHoatDong());
        if (existingHoatDong1.isPresent()) {
            throw new EntityExistsException("hoatdong-exist");
        }
        HoatDong hoatDong = existingHoatDong.get();
        // cập nhật giờ tích lu tổ chuc cho giảng viên cũ
        List<GiangVien> dsGiangVienToChucCu = hoatDong.getGiangVienToChucs();
        String namHoc = String.valueOf(hoatDong.getThoiGianBatDau().getYear());
        for (GiangVien giangVienToChuc : dsGiangVienToChucCu) {
            int gioTichLuyToChuc = hoatDong.getGioTichLuyToChuc();
            GioTichLuy gioTichLuyToChucEntity = gioTichLuyRepository.findByGiangVien_MaTaiKhoanAndNam(giangVienToChuc.getMaTaiKhoan(), namHoc);
            gioTichLuyToChucEntity.setTongSoGio(gioTichLuyToChucEntity.getTongSoGio() - gioTichLuyToChuc);
            gioTichLuyRepository.save(gioTichLuyToChucEntity);
        }
        hoatDong.setTenHoatDong(hoatDongResponse.getTenHoatDong());
        hoatDong.setMoTa(hoatDongResponse.getMoTa());
        hoatDong.setDiaDiem(hoatDongResponse.getDiaDiem());
        hoatDong.setGioTichLuyThamGia(hoatDongResponse.getGioTichLuyThamGia());
        hoatDong.setGioTichLuyToChuc(hoatDongResponse.getGioTichLuyToChuc());
        hoatDong.setThoiGianBatDau(hoatDongResponse.getThoiGianBatDau());
        hoatDong.setThoiGianKetThuc(hoatDongResponse.getThoiGianKetThuc());
        hoatDong.setLoaiHoatDong(loaiHoatDong.get());

        List<GiangVien> giangVienToChucs = giangVienRepository.findByTaiKhoan_TenDangNhapIn(hoatDongResponse.getGiangVienToChucs());
        hoatDong.setGiangVienToChucs(giangVienToChucs);
        hoatDong.setTenQuyetDinh(hoatDongResponse.getTenQuyetDinh());
        hoatDong.setSoQuyetDinh(hoatDongResponse.getSoQuyetDinh());
        hoatDong.setCapToChuc(hoatDongResponse.getCapToChuc());
        hoatDong.setNguoiKyQuyetDinh(hoatDongResponse.getNguoiKyQuyetDinh());

        hoatDongRepository.save(hoatDong);
        // cập nhật giờ tích lu tổ chuc cho giảng viên
        List<GiangVien> dsGiangVienToChucMoi = hoatDong.getGiangVienToChucs();
        String nam = String.valueOf(hoatDong.getThoiGianBatDau().getYear());
        for (GiangVien giangVienToChuc : dsGiangVienToChucMoi) {
            int gioTichLuyToChuc = hoatDong.getGioTichLuyToChuc();
            GioTichLuy gioTichLuyToChucEntity = gioTichLuyRepository.findByGiangVien_MaTaiKhoanAndNam(giangVienToChuc.getMaTaiKhoan(), nam);

            if (gioTichLuyToChucEntity == null) {
                gioTichLuyToChucEntity = new GioTichLuy();
                gioTichLuyToChucEntity.setGiangVien(giangVienToChuc);
                gioTichLuyToChucEntity.setTongSoGio(gioTichLuyToChuc);
                gioTichLuyToChucEntity.setNam(namHoc);
            } else {
                gioTichLuyToChucEntity.setTongSoGio(gioTichLuyToChucEntity.getTongSoGio() + gioTichLuyToChuc);
            }
            gioTichLuyRepository.save(gioTichLuyToChucEntity);
        }
        return hoatDong;
    }

    public void deleteHoatDongById(Long maHoatDong) {
        Optional<HoatDong> hoatDongOptional = hoatDongRepository.findById(maHoatDong);
        if (hoatDongOptional.isPresent()) {
            HoatDong hoatDong = hoatDongOptional.get();
            // cập nhật giờ tích lu tổ chuc cho giảng viên
            List<GiangVien> dsGiangVienToChucs = hoatDong.getGiangVienToChucs();
            String namHoc = String.valueOf(hoatDong.getThoiGianBatDau().getYear());
            for (GiangVien giangVienToChuc : dsGiangVienToChucs) {
                int gioTichLuyToChuc = hoatDong.getGioTichLuyToChuc();
                GioTichLuy gioTichLuyToChucEntity = gioTichLuyRepository.findByGiangVien_MaTaiKhoanAndNam(giangVienToChuc.getMaTaiKhoan(), namHoc);
                gioTichLuyToChucEntity.setTongSoGio(gioTichLuyToChucEntity.getTongSoGio() - gioTichLuyToChuc);
                gioTichLuyRepository.save(gioTichLuyToChucEntity);
            }
            //xóa file quyết định cũ
            String oldFile = hoatDong.getFileQuyetDinh();
            if (oldFile != null) {
                storageService.delete(oldFile);
            }
            hoatDongRepository.deleteById(maHoatDong);
        } else {
            throw new EntityNotFoundException("hoatdong-notfound");
        }

    }

    public List<String> getYears() {
        List<Integer> years = hoatDongRepository.findYears();
        List<String> result = new ArrayList<>();
        for (Integer year : years) {
            result.add(year + "-" + (year + 1));
        }
        return result;
    }

    public List<Integer> getYears1() {
        List<Integer> years = hoatDongRepository.findYears();

        return years;
    }

    public List<HoatDong> getAllHoatDongs() {
        return hoatDongRepository.findAll();
    }

    public Long countUpcomingActivities() {
        return hoatDongRepository.countByTrangThaiHoatDong(HoatDong.TrangThaiHoatDong.SAP_DIEN_RA);
    }
    public List<HoatDong> layDanhSachHoatDongTocChucCuaGiangVien(Long maTaiKhoan, int nam) {
        return hoatDongRepository.findHoatDongTocChucByGiangVienAndYear(maTaiKhoan, nam);
    }
    public HoatDongTongHopResponse getHoatDongInfo(Long maGiangVien, int nam) {
        HoatDongTongHopResponse response = new HoatDongTongHopResponse();

        List<HoatDong> hoatDongs = dangKyHoatDongRepository.findHoatDongByGiangVienAndYear(maGiangVien, nam);
        List<HoatDongNgoaiTruong> hoatDongNgoaiTruongs = hoatDongNgoaiTruongRepository.findHoatDongNgoaiTruongByGiangVienAndYear(maGiangVien, nam);
        List<HoatDong> hoatDongTocChucs = hoatDongRepository.findHoatDongTocChucByGiangVienAndYear(maGiangVien, nam);

        List<HoatDongDTO> hoatDongDTOs = new ArrayList<>();

        for (HoatDong hd : hoatDongs) {
            HoatDongDTO dto = new HoatDongDTO();
            dto.setTenHoatDong(hd.getTenHoatDong());
            dto.setDiaDiem(hd.getDiaDiem());
            dto.setSoGioTichLuy(hd.getGioTichLuyThamGia());
            dto.setLoaiHoatDong("Trong trường");
            dto.setVaiTro("Tham gia");
            dto.setThoiGianBatDau(hd.getThoiGianBatDau());
            dto.setThoiGianKetThuc(hd.getThoiGianKetThuc());
            hoatDongDTOs.add(dto);
        }

        for (HoatDongNgoaiTruong hdn : hoatDongNgoaiTruongs) {
            HoatDongDTO dto = new HoatDongDTO();
            dto.setTenHoatDong(hdn.getTenHoatDong());
            dto.setDiaDiem(hdn.getDiaDiem());
            dto.setSoGioTichLuy(hdn.getGioTichLuyThamGia());
            dto.setLoaiHoatDong("Ngoài trường");
            dto.setVaiTro("Tham gia");
            dto.setThoiGianBatDau(hdn.getThoiGianBatDau());
            dto.setThoiGianKetThuc(hdn.getThoiGianKetThuc());
            hoatDongDTOs.add(dto);
        }

        for (HoatDong hd : hoatDongTocChucs) {
            HoatDongDTO dto = new HoatDongDTO();
            dto.setTenHoatDong(hd.getTenHoatDong());
            dto.setDiaDiem(hd.getDiaDiem());
            dto.setSoGioTichLuy(hd.getGioTichLuyToChuc());
            dto.setLoaiHoatDong("Trong trường");
            dto.setVaiTro("Tổ chức");
            hoatDongDTOs.add(dto);
            dto.setThoiGianBatDau(hd.getThoiGianBatDau());
            dto.setThoiGianKetThuc(hd.getThoiGianKetThuc());
        }

        response.setDanhSachHoatDong(hoatDongDTOs);

        // Lấy tongSoGio và gioBatBuoc
        GiangVien giangVien = giangVienRepository.findById(maGiangVien).orElse(null);
        if (giangVien != null) {

            GioTichLuy gioTichLuy= gioTichLuyRepository.findByGiangVien_MaTaiKhoanAndNam(maGiangVien, String.valueOf(nam));
            int tongSoGio = 0;
            if (gioTichLuy != null) {
                 tongSoGio = gioTichLuy.getTongSoGio();
            };
            int gioBatBuoc = giangVien.getChucDanh().getGioBatBuoc();
            response.setTongSoGio(tongSoGio);
            response.setGioBatBuoc(gioBatBuoc);
        }
        response.setGioHk2(calculateGioForPeriod(maGiangVien, LocalDate.of(nam, 2, 1).atStartOfDay(), LocalDate.of(nam, 6, 30).atTime(23, 59, 59)));
        response.setGioHk3(calculateGioForPeriod(maGiangVien, LocalDate.of(nam, 7, 1).atStartOfDay(), LocalDate.of(nam, 8, 31).atTime(23, 59, 59)));
        response.setGioHk1(calculateGioForPeriod(maGiangVien, LocalDate.of(nam, 9, 1).atStartOfDay(), LocalDate.of(nam + 1, 1, 31).atTime(23, 59, 59)));

        return response;
    }
    private int calculateGioForPeriod(Long maGiangVien, LocalDateTime startDate, LocalDateTime endDate) {
        int totalGio = 0;

        List<HoatDong> hoatDongsThamGia = hoatDongRepository.findHoatDongByGiangVienAndPeriod(maGiangVien, startDate, endDate);
        List<HoatDong> hoatDongsToChuc = hoatDongRepository.findHoatDongTocChucByGiangVienAndPeriod(maGiangVien, startDate, endDate);
        List<HoatDongNgoaiTruong> hoatDongNgoaiTruongs = hoatDongNgoaiTruongRepository.findHoatDongNgoaiTruongByGiangVienAndPeriod(maGiangVien, startDate, endDate);

        for (HoatDong hd : hoatDongsThamGia) {
            totalGio += hd.getGioTichLuyThamGia();
        }
        for (HoatDong hd : hoatDongsToChuc) {
            totalGio += hd.getGioTichLuyToChuc();
        }
        for (HoatDongNgoaiTruong hdn : hoatDongNgoaiTruongs) {
            totalGio += hdn.getGioTichLuyThamGia();
        }

        return totalGio;
    }
}

