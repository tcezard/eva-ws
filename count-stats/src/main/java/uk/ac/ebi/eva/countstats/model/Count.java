package uk.ac.ebi.eva.countstats.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.Type;
import uk.ac.ebi.eva.countstats.configuration.StringJsonUserType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
@Table(name = "count_stats")
public class Count {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NonNull
    @Column(nullable = false)
    private String process;

    @NonNull
    @Column(nullable = false, columnDefinition = "jsonb")
    @Type(StringJsonUserType.class)
    private String identifier;

    @NonNull
    @Column(nullable = false)
    private String metric;

    @NonNull
    @Column(nullable = false)
    private long count;

    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP", insertable = false, updatable = false)
    private LocalDateTime timestamp;
}
