package com.beat.infra.persistence.schedule.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;

import com.beat.domain.schedule.domain.ScheduleNumber;

/**
 * QScheduleJpaEntity is a Querydsl query type for ScheduleJpaEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QScheduleJpaEntity extends EntityPathBase<ScheduleJpaEntity> {

    private static final long serialVersionUID = 1234567890L;

    public static final QScheduleJpaEntity scheduleJpaEntity = new QScheduleJpaEntity("scheduleJpaEntity");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> performanceDate =
        createDateTime("performanceDate", java.time.LocalDateTime.class);

    public final NumberPath<Integer> totalTicketCount = createNumber("totalTicketCount", Integer.class);

    public final NumberPath<Integer> soldTicketCount = createNumber("soldTicketCount", Integer.class);

    public final BooleanPath isBooking = createBoolean("isBooking");

    public final EnumPath<ScheduleNumber> scheduleNumber = createEnum("scheduleNumber", ScheduleNumber.class);

    public final NumberPath<Long> performanceId = createNumber("performanceId", Long.class);

    public QScheduleJpaEntity(String variable) {
        super(ScheduleJpaEntity.class, forVariable(variable));
    }

    public QScheduleJpaEntity(Path<? extends ScheduleJpaEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QScheduleJpaEntity(PathMetadata metadata) {
        super(ScheduleJpaEntity.class, metadata);
    }

}
