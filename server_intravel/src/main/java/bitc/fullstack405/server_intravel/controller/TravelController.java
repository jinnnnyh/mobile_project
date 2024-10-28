package bitc.fullstack405.server_intravel.controller;

import bitc.fullstack405.server_intravel.entity.TravelEntity;
import bitc.fullstack405.server_intravel.service.TravelService;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/travel")
@RequiredArgsConstructor
public class TravelController {

    private final TravelService travelService;

    @GetMapping("/list")
    public List<TravelEntity> TravelList() {
        System.out.println("서벼 연결");
        return travelService.findAll();
    }

    @PostMapping("/save")
    public TravelEntity save(@RequestBody TravelEntity travelEntity) {
        return travelService.save(travelEntity);
    }

    @PostMapping("/listComplete")
    public List<TravelEntity> TravelListComplete(@RequestBody char travComplete) {
        return travelService.findByTravelComplete(travComplete);
    }

    @PutMapping("/update/{tId}")
    public TravelEntity updateTravel(@PathVariable("tId") Long travId, @RequestBody TravelEntity travelEntity) {
        return travelService.updateTravel(travId, travelEntity);
    }

//    @DeleteMapping("/delete/{id}")
//    public void delete(@PathVariable Long id) {
//        travelService.delete(id);
//    }

    @DeleteMapping("/delete/{tId}")
    public void delete(@PathVariable("tId") Long travId) {
        travelService.delete(travId);
    }
}

