# taken from http://lucasbrasilino.com/posts/Vivado-project-for-version-control-how-to-package-an-ip-from-sources/

# create variables for the project
set name [ lindex $argv 0 ]
set v_source [ lindex $argv 1 ]

# create the project
set ip_project [ create_project -name ${name} -force -dir "./${name}_prj" -ip ]
set_property top Encoder [current_fileset]

# read in source files
read_verilog ${v_source}
update_compile_order -fileset sources_1

# package the ip
ipx::package_project
set ip_core [ipx::current_core]

# add the input axi stream

## create the bus interface
ipx::add_bus_interface in ${ip_core}
set in_bus [ipx::get_bus_interfaces in -of_objects ${ip_core}]

## initialise
set_property abstraction_type_vlnv xilinx.com:interface:axis_rtl:1.0 ${in_bus}
set_property bus_type_vlnv xilinx.com:interface:axis:1.0 ${in_bus}
set_property interface_mode slave ${in_bus}

## add ports
ipx::add_port_map TDATA ${in_bus}
ipx::add_port_map TLAST ${in_bus}
ipx::add_port_map TVALID ${in_bus}
ipx::add_port_map TREADY ${in_bus}

## add signals
set_property physical_name io_in_bits [ipx::get_port_maps TDATA -of_objects ${in_bus}]
set_property physical_name io_in_last [ipx::get_port_maps TLAST -of_objects ${in_bus}]
set_property physical_name io_in_valid [ipx::get_port_maps TVALID -of_objects ${in_bus}]
set_property physical_name io_in_ready [ipx::get_port_maps TREADY -of_objects ${in_bus}]

# add the output axi stream
## create the bus interface
ipx::add_bus_interface out ${ip_core}
set out_bus [ipx::get_bus_interfaces out -of_objects ${ip_core}]

## initialise
set_property abstraction_type_vlnv xilinx.com:interface:axis_rtl:1.0 ${out_bus}
set_property bus_type_vlnv xilinx.com:interface:axis:1.0 ${out_bus}
set_property interface_mode master ${out_bus}

## add ports
ipx::add_port_map TDATA ${out_bus}
ipx::add_port_map TLAST ${out_bus}
ipx::add_port_map TVALID ${out_bus}
ipx::add_port_map TREADY ${out_bus}

## add signals
set_property physical_name io_out_bits [ipx::get_port_maps TDATA -of_objects ${out_bus}]
set_property physical_name io_out_last [ipx::get_port_maps TLAST -of_objects ${out_bus}]
set_property physical_name io_out_valid [ipx::get_port_maps TVALID -of_objects ${out_bus}]
set_property physical_name io_out_ready [ipx::get_port_maps TREADY -of_objects ${out_bus}]

# Associate AXI/AXIS interfaces and reset with clock
set aclk_intf [ipx::get_bus_interfaces clock -of_objects ${ip_core}]
set aclk_assoc_intf [ipx::add_bus_parameter ASSOCIATED_BUSIF $aclk_intf]
set_property value in:out $aclk_assoc_intf
set aclk_assoc_reset [ipx::add_bus_parameter ASSOCIATED_RESET $aclk_intf]
set_property value reset $aclk_assoc_reset

# save the ip
set_property core_revision 2 ${ip_core}
ipx::create_xgui_files ${ip_core}
ipx::update_checksums ${ip_core}
ipx::check_integrity ${ip_core}
ipx::save_core ${ip_core}

# close the project
close_project
# file delete -force "./${name}_prj"
