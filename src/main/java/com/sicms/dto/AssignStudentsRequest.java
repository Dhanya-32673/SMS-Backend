package com.sicms.dto;

import java.util.List;

public class AssignStudentsRequest {

    private List<String> studentIds;

    public List<String> getStudentIds() { return studentIds; }
    public void setStudentIds(List<String> studentIds) { this.studentIds = studentIds; }
}
