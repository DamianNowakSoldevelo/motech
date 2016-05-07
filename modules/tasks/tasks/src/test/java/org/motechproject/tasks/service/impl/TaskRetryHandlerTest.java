package org.motechproject.tasks.service.impl;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.EventRelay;
import org.motechproject.tasks.constants.EventDataKeys;
import org.motechproject.tasks.domain.mds.task.TaskActionInformation;
import org.motechproject.tasks.service.TaskService;

import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.motechproject.tasks.constants.EventSubjects.SCHEDULE_REPEATING_JOB;
import static org.motechproject.tasks.constants.EventSubjects.UNSCHEDULE_REPEATING_JOB;

public class TaskRetryHandlerTest extends TasksTestBase {

    @Mock
    private TaskService taskService;

    @Mock
    private EventRelay eventRelay;

    @InjectMocks
    private TaskRetryHandler taskRetryHandler = new TaskRetryHandler();

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        initTask();
    }

    @Test
    public void shouldScheduleTaskRetriesOnFailure() throws Exception {
        setTriggerEvent();
        setActionEvent();

        task.setNumberOfRetries(5);
        task.setRetryIntervalInMilliseconds(5000);

        actionEvent.setServiceInterface("TestService");
        actionEvent.setServiceMethod("abc");

        when(taskService.findActiveTasksForTriggerSubject(triggerEvent.getSubject())).thenReturn(tasks);
        when(taskService.getActionEventFor(task.getActions().get(0))).thenThrow(new RuntimeException());

        taskRetryHandler.handleTaskRetries(task, createEventParameters(), false);
        ArgumentCaptor<MotechEvent> captorEvent = ArgumentCaptor.forClass(MotechEvent.class);

        verify(eventRelay).sendEventMessage(captorEvent.capture());

        MotechEvent scheduleJobEvent = captorEvent.getValue();
        assertEquals(SCHEDULE_REPEATING_JOB, scheduleJobEvent.getSubject());

        Map<String, Object> parameters = scheduleJobEvent.getParameters();
        assertEquals(5, parameters.get(EventDataKeys.REPEAT_COUNT));
        // We send repeat interval time in seconds
        assertEquals(5, parameters.get(EventDataKeys.REPEAT_INTERVAL_TIME));
        assertEquals(task.getId(), parameters.get(EventDataKeys.TASK_ID));
        assertEquals(task.getTrigger().getEffectiveListenerRetrySubject(), parameters.get(EventDataKeys.JOB_SUBJECT));
    }

    @Test
    public void shouldNotScheduleTaskRetriesAgainOnFailure() throws Exception {
        setTriggerEvent();
        setActionEvent();

        task.setNumberOfRetries(5);
        task.setRetryIntervalInMilliseconds(5000);

        actionEvent.setServiceInterface("TestService");
        actionEvent.setServiceMethod("abc");

        when(taskService.getTask(5L)).thenReturn(task);
        when(taskService.getActionEventFor(task.getActions().get(0))).thenThrow(new RuntimeException());

        MotechEvent event = createEvent();
        event.getParameters().put(EventDataKeys.TASK_RETRY, true);

        taskRetryHandler.handleTaskRetries(task, event.getParameters(), false);

        // since we already scheduled task retries, we should not send once again schedule job event
        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
    }

    @Test
    public void shouldNotScheduleTaskRetryOnFailureWhenNumberOfRetriesIsZero() throws Exception {
        setTriggerEvent();
        setActionEvent();

        task.setNumberOfRetries(0);
        task.setRetryIntervalInMilliseconds(0);

        actionEvent.setServiceInterface("TestService");
        actionEvent.setServiceMethod("abc");

        when(taskService.findActiveTasksForTriggerSubject(triggerEvent.getSubject())).thenReturn(tasks);
        when(taskService.getActionEventFor(task.getActions().get(0))).thenThrow(new RuntimeException());

        MotechEvent event = createEvent();
        taskRetryHandler.handleTaskRetries(task, event.getParameters(), false);

        // task number of retries is 0, we should not send schedule job event
        verify(eventRelay, never()).sendEventMessage(any(MotechEvent.class));
    }

    @Test
    public void shouldUnscheduleTaskRetriesWhenSuccess() throws Exception {
        setTriggerEvent();
        setActionEvent();

        task.setNumberOfRetries(5);
        task.setRetryIntervalInMilliseconds(5000);

        actionEvent.setServiceInterface("TestService");
        actionEvent.setServiceMethod("abc");

        when(taskService.getTask(5L)).thenReturn(task);
        when(taskService.getActionEventFor(any(TaskActionInformation.class))).thenReturn(actionEvent);

        MotechEvent event = createEvent();
        event.getParameters().put(EventDataKeys.TASK_RETRY, true);

        taskRetryHandler.handleTaskRetries(task, event.getParameters(), true);

        ArgumentCaptor<MotechEvent> captorEvent = ArgumentCaptor.forClass(MotechEvent.class);

        verify(eventRelay).sendEventMessage(captorEvent.capture());

        MotechEvent scheduleJobEvent = captorEvent.getValue();
        assertEquals(UNSCHEDULE_REPEATING_JOB, scheduleJobEvent.getSubject());

        Map<String, Object> parameters = scheduleJobEvent.getParameters();
        assertEquals(task.getTrigger().getEffectiveListenerRetrySubject(), parameters.get(EventDataKeys.JOB_SUBJECT));
    }
}
