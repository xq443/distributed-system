package com.cathy.skier.pojo;

import com.cathy.skier.validation.DayID;
import com.cathy.skier.validation.LiftID;
import com.cathy.skier.validation.ResortID;
import com.cathy.skier.validation.SeasonID;
import com.cathy.skier.validation.SkierID;
import com.cathy.skier.validation.Time;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.*;
import javax.persistence.Table;


@Getter
@Setter
@RequiredArgsConstructor
@Entity
@Table(name = "liftride")
public class LiftRide {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "LiftID", nullable = false)
  @LiftID
  private Integer liftID;

  @Column(name = "resortID", nullable = false)
  @ResortID
  private Integer resortID;

  @Column(name = "seasonID", nullable = false)
  @SeasonID
  private String seasonID;

  @Column(name = "dayID", nullable = false)
  @DayID
  private String dayID;


  @Column(name = "skierID", nullable = false)
  @SkierID
  private Integer skierID;


  @Column(name = "time", nullable = false)
  @Time
  private Integer time;
}
