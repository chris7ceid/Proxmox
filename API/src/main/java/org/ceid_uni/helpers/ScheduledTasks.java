package org.ceid_uni.helpers;

import org.ceid_uni.models.Request;
import org.ceid_uni.security.services.IPromoxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Component
public class ScheduledTasks {

    private static Environment env;

    private IPromoxService promoxService;

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Scheduled(fixedRate = 60000)
    public void checkForExpiredVms() {
        Date currentDate = new Date();
        logger.info("Task Scheduler checks for actions if needed. The time is now {}", dateFormat.format(currentDate));
        List<Request> requestList = promoxService.findAllRequests();
        if (!requestList.isEmpty()){
            for (Request request : requestList){
                if(!request.getToBeRemoved() && request.getEndDate().compareTo(currentDate.toInstant()) < 0){
                    logger.info("id: " + request.getId() + " Start: " + request.getStartDate().toString()
                            + " End: " + request.getEndDate().toString());
                    logger.info("Request ({}) marked as to be removed", request.getId());
                    request.setToBeRemoved(Boolean.TRUE);
                }
            }
            promoxService.saveRequests(requestList);
        }
    }

    @Autowired
    public void setEnv(Environment env) {
        ScheduledTasks.env = env;
    }

    @Autowired
    public void setPromoxService(IPromoxService promoxService) {
        this.promoxService = promoxService;
    }

}
