package dk.dtu.dto;

/**
 * @author Niklas Emil Lysdal
 */
public class OperationResult
{
    private String status;

    public OperationResult(String status){
        this.status = status;
    }
    ;

    public String getStatus() {return this.status;}

}