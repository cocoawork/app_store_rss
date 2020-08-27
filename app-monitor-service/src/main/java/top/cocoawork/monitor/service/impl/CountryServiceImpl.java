package top.cocoawork.monitor.service.impl;

import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;
import top.cocoawork.monitor.dao.mapper.CountryMapper;
import top.cocoawork.monitor.dao.entity.Country;
import top.cocoawork.monitor.service.api.model.CountryDto;
import top.cocoawork.monitor.service.api.CountryService;
import top.cocoawork.monitor.service.impl.base.BaseServiceImpl;
import top.cocoawork.monitor.util.BeanUtil;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CountryServiceImpl extends BaseServiceImpl<Country, CountryDto> implements CountryService {

    @Autowired
    private CountryMapper countryMapper;

    @Override
    public List<CountryDto> selectAll() {
        List<Country> countries = countryMapper.selectList(null);
        return countries.stream().map(country -> d2dto(country)).collect(Collectors.toList());
    }
}