SELECT V.test_type, 
       ( 100 - ( SUM(V.affected_qty) / SUM(V.order_qty) * 100 ) ) AS YIELD, 
       security_group 
FROM   (SELECT A.order_id, 
               A.order_qty, 
               A.program, 
               A.model, 
               CASE 
                 WHEN A.asgnd_location_id IS NULL THEN NULL 
                 ELSE (SELECT work_loc 
                       FROM   sffnd_work_loc_def LOC 
                       WHERE  LOC.location_id = A.asgnd_location_id) 
               END                                AS ASGND_WORK_LOC, 
               B.actual_end_date, 
               B.oper_key, 
               B.step_key, 
               B.oper_no, 
               B.test_type, 
               CASE 
                 WHEN B.asgnd_center_id IS NULL THEN NULL 
                 ELSE (SELECT work_center 
                       FROM   sffnd_work_center_def CEN 
                       WHERE  CEN.location_id = B.asgnd_location_id 
                              AND CEN.department_id = B.asgnd_department_id 
                              AND CEN.center_id = B.asgnd_center_id) 
               END                                AS ASGND_WORK_CENTER, 
               (SELECT Nvl(SUM(affected_qty), 0) 
                FROM   sfmfg.sfqa_disc_item 
                WHERE  order_id = B.order_id 
                       AND oper_key = B.oper_key) AS AFFECTED_QTY, 
               A.security_group 
        FROM   sfmfg.sfwid_order_desc A, 
               sfmfg.sfwid_oper_desc B, 
               sffnd_oper_type_def C 
        WHERE  C.oper_type_role = 'TEST' 
               AND B.oper_status = 'CLOSE' 
               AND B.step_key = -1 
               AND B.test_type IS NOT NULL 
               AND A.order_id = B.order_id 
               AND C.oper_type = B.oper_type) V 
WHERE  V.asgnd_work_loc = :WORK_LOC 
       AND EXISTS (SELECT 1 
                   FROM   (SELECT column_value 
                           FROM   TABLE( 
           sfmfg.Sffnd_parse_picklist('PROGRAM', :PROGRAM_LIST, ';', NULL))) 
                   WHERE  column_value = Coalesce(V.program, 'N/A')) 
       AND EXISTS (SELECT 1 
                   FROM   (SELECT column_value 
                           FROM 
       TABLE(sfmfg.Sffnd_parse_picklist('MODEL', :MODEL_LIST, ';', NULL))) 
       WHERE  column_value = Nvl(V.model, 'N/A')) 
       AND EXISTS (SELECT 1 
                   FROM   (SELECT column_value 
                           FROM 
       TABLE( 
sfmfg.Sffnd_parse_picklist('WORK_CENTER', :WORK_CENTER_LIST, ';', :WORK_LOC))) 
WHERE  column_value = Nvl(V.asgnd_work_center, 'N/A')) 
AND Trunc(V.actual_end_date) >= To_date(:SCH_START_DATE, 'MM/DD/YYYY') 
AND Trunc(V.actual_end_date) <= To_date(:SCH_END_DATE, 'MM/DD/YYYY') 
GROUP  BY V.test_type, 
          security_group 