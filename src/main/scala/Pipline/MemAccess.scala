package Pipline
import chisel3._
import chisel3.util.MuxCase
import common.Defines._
import connect.{CsrIO, ExMemIO, WbIO, MaWbIO}
class MemAccess extends Module {
  val io = IO(new Bundle() {
    val extend = Flipped(new ExMemIO)
    val csr = Flipped(new CsrIO(CSR_REG_LEN))
    val wbio = Flipped(new WbIO)
    val passby = new MaWbIO
  })

  // 定义一组寄存器，连接execute与memory access阶段
  val mem_pc_reg = RegInit(0.U(WORD_LEN_WIDTH))
  val wb_addr_reg = RegInit(0.U(REG_ADDR_WIDTH))
  val op1_data_reg = RegInit(UInt(WORD_LEN_WIDTH))
  val rs2_data_reg = RegInit(UInt(WORD_LEN_WIDTH))
  val mem_wen_reg = RegInit(UInt(MEN_LEN))
  val reg_wen_reg = RegInit(UInt(REN_LEN))
  val wb_sel_reg = RegInit(UInt(WB_SEL_LEN))
  val csr_addr_reg = RegInit(UInt(CSR_REG_WIDTH))
  val csr_cmd_reg = RegInit(UInt(CSR_LEN))
  val imm_z_uext_reg = RegInit(UInt(WORD_LEN_WIDTH))
  val alu_out_reg = RegInit(UInt(WORD_LEN_WIDTH))

  mem_pc_reg := io.extend.exe_pc_reg
  wb_addr_reg := io.extend.wb_addr
  op1_data_reg := io.extend.op1_data
  rs2_data_reg := io.extend.rs2_data
  mem_wen_reg := io.extend.mem_wen
  reg_wen_reg := io.extend.reg_wen
  reg_wen_reg := io.extend.reg_wen
  wb_sel_reg := io.extend.wb_sel
  csr_addr_reg := io.extend.csr_addr
  csr_cmd_reg := io.extend.csr_cmd
  imm_z_uext_reg := io.extend.imm_z_uext
  alu_out_reg := io.extend.alu_out

  // 连接访存与memory
  io.wbio.wdata := alu_out_reg
  io.wbio.write_en := mem_wen_reg
  io.wbio.wdata := rs2_data_reg

  // 连接访存与CSR
  // CSR: 取数和写数
  io.csr.reg_raddr := csr_addr_reg
  io.csr.reg_wdata := csr_wdata
  val csr_rdata = io.csr.reg_rdata
  val csr_wdata = MuxCase(0.U(WORD_LEN_WIDTH), Seq(
    (csr_cmd_reg === CSR_W) -> op1_data_reg,
    (csr_cmd_reg === CSR_S) -> (op1_data_reg | csr_rdata),
    (csr_cmd_reg === CSR_C) -> ((~op1_data_reg).asUInt & csr_rdata),
    (csr_cmd_reg === CSR_E) -> 11.U(WORD_LEN_WIDTH)
  ))
  when(csr_cmd_reg > 0.U(CSR_LEN)) {
    io.csr.wen := true.asBool
  }.otherwise{
    io.csr.wen := false.asBool
  }

  val mem_wb_data = MuxCase(alu_out_reg, Seq(
    (wb_sel_reg === WB_MEM) -> io.wbio.rdata, // 将从mem中读到的数据作为要写入的data
    (wb_sel_reg === WB_PC) -> (mem_pc_reg + 4.U(WORD_LEN_WIDTH)),
    (wb_sel_reg === WB_CSR) -> csr_rdata
  ))

  // 连接写回环节
  io.passby.reg_wb_addr := wb_addr_reg
  io.passby.reg_wen := reg_wen_reg
  io.passby.reg_wb_data := mem_wb_data

}
