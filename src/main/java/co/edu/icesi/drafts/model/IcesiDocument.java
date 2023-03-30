package co.edu.icesi.drafts.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IcesiDocument {

    @Id
    @Type(type = "uuid-char")
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private UUID icesiDocumentId;
    private String title;
    private String text;
    private IcesiDocumentStatus status;
    @ManyToOne(optional = false)
    @JoinColumn(name = "icesi_user_documents")
    private IcesiUser icesiUser;

}
