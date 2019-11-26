package com.example.activitiexplorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MyFirstProcessTest {
    Logger log = LoggerFactory.getLogger(MyFirstProcessTest.class);
    @Resource
    RepositoryService repositoryService;
    @Resource
    RuntimeService runtimeService;
    @Resource
    TaskService taskService;
    @Resource
    HistoryService historyService;
    @Resource
    ProcessEngine processEngine;


    private String dateToString(Date date, String format) {
        return new SimpleDateFormat(format).format(date);
    }

    /**
     * 流程部署
     * 涉及act_re_deployment、act_re_procdef、act_ge_bytearray
     */
    /*@Test
    public void contextLoads() {
        // 部署一个流程定义
        Deployment deployment = repositoryService
                .createDeployment()  // 创建一个Deployment
                .name("测试分支流程")     // 给流程起的一个名字
                .addClasspathResource("processes/BranchProcess.bpmn")    // bpmn的路径
                .deploy();
        log.info("流程部署ID：" + deployment.getId());
        log.info("流程部署时间：" + dateToString(deployment.getDeploymentTime(), "yyyy-MM-dd HH:mm:ss"));
        log.info("流程部署名称：" + deployment.getName());
    }*/

    /**
     * 流程部署
     *
     * @throws IOException
     */
    @Test
    public void methodEntity() throws IOException {
        ObjectNode modelNode = (ObjectNode) new ObjectMapper().readTree(repositoryService.getModelEditorSource("27501"));
        byte[] bpmnBytes;
        BpmnModel bpmnModel = new BpmnJsonConverter().convertToBpmnModel(modelNode);
        bpmnBytes = new BpmnXMLConverter().convertToXML(bpmnModel);
        String processName = "branchTest" + ".bpmn20.xml";
        Deployment deployment = repositoryService.createDeployment().name("分支流程测试3").addString(processName, new String(bpmnBytes, "utf-8")).deploy();
        log.info("流程部署ID：" + deployment.getId());
        log.info("流程部署时间：" + dateToString(deployment.getDeploymentTime(), "yyyy-MM-dd HH:mm:ss"));
        log.info("流程部署名称：" + deployment.getName());
    }

    /**
     * 启动流程
     * act_hi_actinst、act_hi_identitylink、act_hi_procinst、act_hi_taskinst、act_ru_execution
     * act_ru_identitylink、act_ru_task
     */
    @Test
    public void startProcessInstance() {
        // 启动流程
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("branchProcess");//key值来自act_re_procdef
        log.info("流程实例ID:" + pi.getId());//流程实例ID
        log.info("流程定义ID:" + pi.getProcessDefinitionId());//流程定义ID
    }

    /**
     * 查询流程定义act_re_procdef
     */
    @Test
    public void findProcessDefinition() {
        List<ProcessDefinition> list = processEngine.getRepositoryService()//与流程定义和部署对象相关的Service
                .createProcessDefinitionQuery()//创建一个流程定义的查询
                /**指定查询条件,where条件*/
//                      .deploymentId(deploymentId)//使用部署对象ID查询
//                      .processDefinitionId(processDefinitionId)//使用流程定义ID查询
//                      .processDefinitionKey(processDefinitionKey)//使用流程定义的key查询
//                      .processDefinitionNameLike(processDefinitionNameLike)//使用流程定义的名称模糊查询

                /**排序*/
                .orderByProcessDefinitionVersion().asc()//按照版本的升序排列
//                      .orderByProcessDefinitionName().desc()//按照流程定义的名称降序排列

                /**返回的结果集*/
                .list();//返回一个集合列表，封装流程定义
//                      .singleResult();//返回惟一结果集
//                      .count();//返回结果集数量
//                      .listPage(firstResult, maxResults);//分页查询
        if (list != null && list.size() > 0) {
            //对应act_re_procdef表中的数据
            for (ProcessDefinition pd : list) {
                log.info("流程定义ID:" + pd.getId());//流程定义的key+版本+随机生成数
                log.info("流程定义的名称:" + pd.getName());//对应myFirstProcess.bpmn文件中的name属性值
                log.info("流程定义的key:" + pd.getKey());//对应myFirstProcess.bpmn文件中的id属性值
                log.info("流程定义的版本:" + pd.getVersion());//当流程定义的key值相同的相同下，版本升级，默认1
                log.info("资源名称bpmn文件:" + pd.getResourceName());
                log.info("资源名称png文件:" + pd.getDiagramResourceName());
                log.info("部署对象ID：" + pd.getDeploymentId());
                log.info("*********************************************");
            }
        }
    }

    /**
     * 查看流程图
     *
     * @throws IOException
     */
    @Test
    public void viewPic() throws IOException {
        /**将生成图片放到文件夹下*/
        String deploymentId = "32501";//对应act_re_deployment的ID_
        //获取图片资源名称
        List<String> list = processEngine.getRepositoryService()
                .getDeploymentResourceNames(deploymentId);
        //定义图片资源的名称
        String resourceName = "processImage";
        if (list != null && list.size() > 0) {
            for (String name : list) {
                if (name.indexOf(".png") >= 0) {
                    resourceName = name;
                }
            }
        }
        //获取图片的输入流
        InputStream in = processEngine.getRepositoryService()
                .getResourceAsStream(deploymentId, resourceName);
        //将图片生成到F盘的目录下
        File file = new File("F:/" + resourceName);
        //将输入流的图片写到磁盘
        FileUtils.copyInputStreamToFile(in, file);
    }

    /**
     * 查询最新版本的流程定义
     */
    @Test
    public void findLastVersionProcessDefinition() {
        List<ProcessDefinition> list = processEngine.getRepositoryService()//
                .createProcessDefinitionQuery()//
                .orderByProcessDefinitionVersion().asc()//使用流程定义的版本升序排列
                .list();
        /**
         map集合的特点：当map集合key值相同的情况下，后一次的值将替换前一次的值
         */
        Map<String, ProcessDefinition> map = new LinkedHashMap<>();
        if (list != null && list.size() > 0) {
            for (ProcessDefinition pd : list) {
                map.put(pd.getKey(), pd);
            }
        }
        List<ProcessDefinition> pdList = new ArrayList<>(map.values());
        if (pdList != null && pdList.size() > 0) {
            for (ProcessDefinition pd : pdList) {
                log.info("流程定义ID:" + pd.getId());//流程定义的key+版本+随机生成数
                log.info("流程定义的名称:" + pd.getName());//对应hello.bpmn文件中的name属性值
                log.info("流程定义的key:" + pd.getKey());//对应hello.bpmn文件中的id属性值
                log.info("流程定义的版本:" + pd.getVersion());//当流程定义的key值相同的相同下，版本升级，默认1
                log.info("资源名称bpmn文件:" + pd.getResourceName());
                log.info("资源`名称png文件:" + pd.getDiagramResourceName());
                log.info("部署对象ID：" + pd.getDeploymentId());
                log.info("*********************************************************************************");
            }
        }
    }

    /**
     * 查询当前人的个人任务act_ru_task
     */
    @Test
    public void findPersonalTask() {
        String assignee = "projManagerName";
        List<Task> list = processEngine.getTaskService()//与正在执行的任务管理相关的Service
                .createTaskQuery()//创建任务查询对象
                /**查询条件（where部分）*/
                .taskAssignee(assignee)//指定个人任务查询，指定办理人
//                      .taskCandidateUser(candidateUser)//组任务的办理人查询
//                      .processDefinitionId(processDefinitionId)//使用流程定义ID查询
//                      .processInstanceId(processInstanceId)//使用流程实例ID查询
//                      .executionId(executionId)//使用执行对象ID查询
                /**排序*/
                .orderByTaskCreateTime().asc()//使用创建时间的升序排列
                /**返回结果集*/
//                      .singleResult()//返回惟一结果集
//                      .count()//返回结果集的数量
//                      .listPage(firstResult, maxResults);//分页查询
                .list();//返回列表
        if (list != null && list.size() > 0) {
            for (Task task : list) {
                log.info("任务ID:" + task.getId());
                log.info("任务名称:" + task.getName());
                log.info("任务的创建时间:" + task.getCreateTime());
                log.info("任务的办理人:" + task.getAssignee());
                log.info("流程实例ID：" + task.getProcessInstanceId());
                log.info("执行对象ID:" + task.getExecutionId());
                log.info("流程定义ID:" + task.getProcessDefinitionId());
                log.info("********************************************");
            }
        }
    }

    /**
     * 完成我的任务
     */
    @Test
    public void completePersonalTask() {
        //任务ID，上一步查询得到的。
        String taskId = "72505";
        Map<String, Object> vars = new HashMap<>();
        vars.put("rejectFlag2", "1");
        vars.put("approveFlag2", "0");
        taskService.addComment(taskId, "70001", "部门经理审批不通过");
        taskService.complete(taskId, vars);//与正在执行的任务管理相关的Service
        log.info("完成任务：任务ID：" + taskId);
    }

    /**
     * 查询流程状态（判断流程走到哪一个节点）
     */
    @Test
    public void isProcessActive() {
        String processInstanceId = "35001";
        ProcessInstance pi = processEngine.getRuntimeService()//表示正在执行的流程实例和执行对象
                .createProcessInstanceQuery()//创建流程实例查询
                .processInstanceId(processInstanceId)//使用流程实例ID查询
                .singleResult();
        if (pi == null) {
            log.info("流程已经结束");
        } else {
            log.info("流程没有结束");
            //获取任务状态
            log.info("节点id：" + pi.getActivityId());
        }
    }

    /**
     * 查看当前流程图的所在节点(单线流程，一个执行对象的时候)
     */
    @Test
    public void getCurrentView() {
        ActivityImpl activity = null;
        String processDefinitionId = "branchProcess:1:4";//流程定义id
        String processInstanceId = "7501";//流程实例id
        ExecutionEntity entity = (ExecutionEntity) runtimeService.createExecutionQuery().processInstanceId(processInstanceId).singleResult();
        String activityId = entity.getActivityId();
        ProcessDefinitionEntity definitionEntity = (ProcessDefinitionEntity) repositoryService.getProcessDefinition(processDefinitionId);
        List<ActivityImpl> activities = definitionEntity.getActivities();
        for (ActivityImpl activityImpl : activities) {
            if (activityId.equals(activityImpl.getId())) {
                activity = activityImpl;
                break;
            }
        }
        log.info("aaaa:" + activity);
    }

    /**
     * 历史活动查询接口act_hi_actinst
     */
    @Test
    public void findHistoryActivity() {
        String processInstanceId = "12501";
        List<HistoricActivityInstance> hais = processEngine.getHistoryService()
                .createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .list();
        for (HistoricActivityInstance hai : hais) {
            log.info("活动id：" + hai.getActivityId()
                    + "   审批人：" + hai.getAssignee()
                    + "   任务id：" + hai.getTaskId());
            log.info("************************************");
        }
    }

    /**
     * 查询历史流程实例act_hi_procinst
     */
    @Test
    public void findHistoryProcessInstance() {
        String processInstanceId = "2501";
        HistoricProcessInstance hpi = processEngine.getHistoryService()// 与历史数据（历史表）相关的Service
                .createHistoricProcessInstanceQuery()// 创建历史流程实例查询
                .processInstanceId(processInstanceId)// 使用流程实例ID查询
                .orderByProcessInstanceStartTime().asc().singleResult();
        log.info("实例id:" + hpi.getId());
        log.info("实例定义id:" + hpi.getProcessDefinitionId());
        log.info("开始时间:" + dateToString(hpi.getStartTime(), "yyyy-MM-dd HH:mm:ss"));

        log.info("结束时间:" + dateToString(hpi.getEndTime(), "yyyy-MM-dd HH:mm:ss"));
        log.info("时长:" + hpi.getDurationInMillis());
    }


    /**
     * 查询历史任务act_hi_taskinst
     */
    @Test
    public void findHistoryTask() {
        String processInstanceId = "2501";
        List<HistoricTaskInstance> list = processEngine.getHistoryService()// 与历史数据（历史表）相关的Service
                .createHistoricTaskInstanceQuery()// 创建历史任务实例查询
                .processInstanceId(processInstanceId)//
                .orderByHistoricTaskInstanceStartTime().asc().list();
        if (list != null && list.size() > 0) {
            for (HistoricTaskInstance hti : list) {
                log.info("任务Id：" + hti.getId());
                log.info("任务名称：" + hti.getName());
                log.info("流程实例Id：" + hti.getProcessInstanceId());
                log.info("开始时间：" + dateToString(hti.getStartTime(), "yyyy-MM-dd HH:mm:ss"));
                log.info("结束时间：" + dateToString(hti.getEndTime(), "yyyy-MM-dd HH:mm:ss"));
                log.info("持续时间：" + hti.getDurationInMillis());
                log.info("*******************************************");
            }
        }
    }

    /**
     * 查询历史流程变量
     */
    @Test
    public void findHistoryProcessVariables() {
        String processInstanceId = "2501";
        List<HistoricVariableInstance> list = processEngine.getHistoryService()//
                .createHistoricVariableInstanceQuery()// 创建一个历史的流程变量查询对象
                .processInstanceId(processInstanceId)//
                .list();
        if (list != null && list.size() > 0) {
            for (HistoricVariableInstance hvi : list) {
                log.info("\n" + hvi.getId() + "   " + hvi.getProcessInstanceId() + "\n" + hvi.getVariableName()
                        + "   " + hvi.getVariableTypeName() + "    " + hvi.getValue());
            }
        }
    }

    /**
     * 通过执行sql来查询历史数据，由于activiti底层就是数据库表。
     */
    @Test
    public void findHistoryByNative() {
        HistoricProcessInstance hpi = processEngine.getHistoryService()
                .createNativeHistoricProcessInstanceQuery()
                .sql("SELECT t.ID_,t.PROC_DEF_ID_,t.START_TIME_,t.END_TIME_,t.DURATION_ FROM `act_hi_procinst` t")
                .singleResult();
        log.info("实例id:" + hpi.getId());
        log.info("实例定义id:" + hpi.getProcessDefinitionId());
        log.info("开始时间:" + dateToString(hpi.getStartTime(), "yyyy-MM-dd HH:mm:ss"));

        log.info("结束时间:" + dateToString(hpi.getEndTime(), "yyyy-MM-dd HH:mm:ss"));
        log.info("时长:" + hpi.getDurationInMillis());
    }

}
