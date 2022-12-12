package com.controller;


import com.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class SampleController {

  @Autowired
  private SampleService sampleService;


  @RequestMapping(method = RequestMethod.POST)
  public void testTransaction(){

    //sampleService.testTransaction();

    //sampleService.testTransactionFailure(); //working right

//    sampleService.driverAddTest();

//    sampleService.testBothDBInsert();

       sampleService.testTemplate();
  }
}
