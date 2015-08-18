SELECT order_part_issue_id,
       order_id, oper_key, issued_oper_no, step_key, plnd_item_id, ref_des,
       issued_part_no, issued_part_chg, issued_item_id, issued_qty, issued_serial_no, issued_lot_no,
       issued_spool_no, issued_work_loc, issued_work_dept, issued_work_center, store_loc, issued_external_erp_no, 
       issued_consumed, updt_userid, time_stamp, last_action, comments,
       :ALT_PART_FLAG AS ALT_PART_FLAG,
       issued_part_no as issue_alt_part_no,
       issued_part_chg as issue_alt_part_chg,
       issued_item_id AS issue_alt_item_id,
       :BOM_LINE_NO as order_bom_line_no,
       bom_line_no as bom_line_no,
       PHANTOM_KIT_PART_NO, PHANTOM_KIT_NO,
       ucf_item_issued_vch1, ucf_item_issued_vch2, ucf_item_issued_vch3, ucf_item_issued_vch4, ucf_item_issued_vch5,
       ucf_item_issued_vch6, ucf_item_issued_vch7, ucf_item_issued_vch8, ucf_item_issued_vch9, ucf_item_issued_vch10,
       ucf_item_issued_vch11, ucf_item_issued_vch12, ucf_item_issued_vch13, ucf_item_issued_vch14, ucf_item_issued_vch15,
       ucf_item_issued_num1, ucf_item_issued_num2, ucf_item_issued_num3, ucf_item_issued_num4, ucf_item_issued_num5,
       ucf_item_issued_flag1, ucf_item_issued_flag2, ucf_item_issued_flag3, ucf_item_issued_flag4, ucf_item_issued_flag5,
       ucf_item_issued_date1, ucf_item_issued_date2, ucf_item_issued_date3, ucf_item_issued_date4, ucf_item_issued_date5,
       ucf_item_issued_vch255_1, ucf_item_issued_vch255_2, ucf_item_issued_vch255_3, ucf_item_issued_vch4000_1, ucf_item_issued_vch4000_2
  FROM (SELECT order_part_issue_id,
               a.order_id, a.oper_key, b.oper_no as issued_oper_no, a.step_key,
               CASE
                 WHEN a.plnd_item_id IS NULL THEN
                  a.issued_item_id
                 ELSE
                  a.plnd_item_id
               END AS plnd_item_id,
               a.ref_des,
               issued_part_no, issued_part_chg, issued_item_id, issued_qty, issued_serial_no, issued_lot_no, issued_spool_no, 
               issued_work_loc, issued_work_dept, issued_work_center, store_loc, issued_external_erp_no, issued_consumed, 
               a.updt_userid, a.time_stamp, a.last_action, a.comments,a.bom_line_no, a.PHANTOM_KIT_PART_NO, a.PHANTOM_KIT_NO,
               ucf_item_issued_vch1, ucf_item_issued_vch2, ucf_item_issued_vch3, ucf_item_issued_vch4, ucf_item_issued_vch5,
               ucf_item_issued_vch6, ucf_item_issued_vch7, ucf_item_issued_vch8, ucf_item_issued_vch9, ucf_item_issued_vch10,
               ucf_item_issued_vch11, ucf_item_issued_vch12, ucf_item_issued_vch13, ucf_item_issued_vch14, ucf_item_issued_vch15,
               ucf_item_issued_num1, ucf_item_issued_num2, ucf_item_issued_num3, ucf_item_issued_num4, ucf_item_issued_num5,
               ucf_item_issued_flag1, ucf_item_issued_flag2, ucf_item_issued_flag3, ucf_item_issued_flag4, ucf_item_issued_flag5,
               ucf_item_issued_date1, ucf_item_issued_date2, ucf_item_issued_date3, ucf_item_issued_date4, ucf_item_issued_date5,
               ucf_item_issued_vch255_1, ucf_item_issued_vch255_2, ucf_item_issued_vch255_3, ucf_item_issued_vch4000_1, ucf_item_issued_vch4000_2
          FROM sfwid_order_item_issued a left outer join sfwid_oper_desc b on
          a.order_id = b.order_id and a.oper_key = b.oper_key and a.step_key = b.step_key
          WHERE (plnd_item_id = :PLND_ITEM_ID or
            (a.plnd_item_id is null AND
               (A.ISSUED_ITEM_ID = :PLND_ITEM_ID OR
                 EXISTS
                   (SELECT 'X' FROM sfmfg.sfwid_order_item_alt_xref ALT
                    WHERE ORDER_ID = :ORDER_ID
                    AND oper_key = :OPER_KEY
                    AND ALT.PLND_ITEM_ID = :PLND_ITEM_ID
                    AND ALT.ASGND_ITEM_ID = A.ISSUED_ITEM_ID )
             ) ) )
          )
 WHERE order_id = :order_id
   AND (oper_key = :oper_key or OPER_KEY IS NULL)