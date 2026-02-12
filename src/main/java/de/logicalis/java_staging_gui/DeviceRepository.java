package de.logicalis.java_staging_gui;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findByProjectTitle(String projectTitle);

    boolean existsByProjectTitleAndHostname(String projectTitle, String hostname);

    boolean existsByProjectTitleAndIp(String projectTitle, String ip);

    boolean existsByProjectTitleAndUpstreamSwitchAndUpstreamPort(
            String projectTitle, String upstreamSwitch, int upstreamPort);

    // conditional status change: only applies while the status is still :from.
    // returns the number of changed rows (1 = won, 0 = someone else was faster).
    // makes the OPEN->IN_PROGRESS transition atomic against concurrent start requests.
    @Modifying(clearAutomatically = true)
    @Query("update Device d set d.status = :to, d.currentStep = :step, "
            + "d.errorMessage = null, d.startedAt = :now "
            + "where d.id = :id and d.status = :from")
    int updateStatusIfCurrent(@Param("id") long id,
                              @Param("from") DeviceStatus from,
                              @Param("to") DeviceStatus to,
                              @Param("step") String step,
                              @Param("now") long now);
}
