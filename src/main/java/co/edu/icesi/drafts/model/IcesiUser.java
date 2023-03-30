package co.edu.icesi.drafts.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcesiUser {

    @Id
    @Type(type = "uuid-char")
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private UUID icesiUserId;
    private String firstName;
    private String lastName;
    private String code;
    private String email;
    private String phoneNumber;
    @OneToMany(mappedBy = "icesiUser")
    private List<IcesiDocument> documents;

}
