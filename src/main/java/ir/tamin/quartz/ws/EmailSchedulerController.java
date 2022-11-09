package ir.tamin.quartz.ws;

import ir.tamin.quartz.payload.EmailRequest;
import ir.tamin.quartz.payload.EmailResponse;
import ir.tamin.quartz.quartz.EmailJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Date;
import java.time.ZonedDateTime;
import java.util.UUID;


@Slf4j
@RestController
public class EmailSchedulerController {

    @Autowired
    private Scheduler scheduler;

    @PostMapping("/schedule/email")
    public ResponseEntity<EmailResponse> scheduleEmail(@RequestBody EmailRequest emailRequest){

        try {
            ZonedDateTime dateTime = ZonedDateTime.of(emailRequest.getLocalDateTime(), emailRequest.getZoneId());
            if (dateTime.isBefore(ZonedDateTime.now())) {
                EmailResponse emailResponse = new EmailResponse(false, "Incorrect Date and Time");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(emailResponse);
            }

            JobDetail jobDetail = buildJobDetail(emailRequest);
            Trigger trigger = buildTrigger(jobDetail, dateTime);
            scheduler.scheduleJob(jobDetail, trigger);
            EmailResponse emailResponse = new EmailResponse(true,
                    jobDetail.getKey().getName(),
                    jobDetail.getKey().getGroup(),
                    "Email scheduled successfully");
            return ResponseEntity.ok(emailResponse);

        } catch (SchedulerException ex){
            log.error("Error occurred while scheduling email: ",ex);
            EmailResponse emailResponse = new EmailResponse(false,"An error occurred while scheduling." +
                    "please try again later.");
            return  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(emailResponse);
        }
    }

    @GetMapping("/get")
    public ResponseEntity<String> getApiTest(){
        return ResponseEntity.ok("Get API Test ---> Pass");
    }


    private JobDetail buildJobDetail(EmailRequest emailRequest){

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("email",emailRequest.getEmail());
        jobDataMap.put("subject",emailRequest.getSubject());
        jobDataMap.put("body",emailRequest.getBody());

        return JobBuilder.newJob(EmailJob.class)
                .withIdentity(UUID.randomUUID().toString(),"email-job")
                .withDescription("Send Email Job")
                .usingJobData(jobDataMap)
                .storeDurably()
                .build();
    }

    private Trigger buildTrigger(JobDetail jobDetail, ZonedDateTime startAt){
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(),"email-trigger")
                .withDescription("send emil trigger")
                .startAt(Date.from(startAt.toInstant()))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withMisfireHandlingInstructionFireNow())
                .build();
    }


}
