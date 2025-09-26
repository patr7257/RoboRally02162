package dk.dtu.dto;

/* Authors: Niklas
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