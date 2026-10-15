//! Read-only FeliCa transport framing and Japanese transit-card history decoding.
//! No network, card writes, card keys or account authentication.
use std::fmt::Write;

#[derive(Debug, PartialEq, Eq)]
pub enum Error { Length, Command, Card, Status(u8,u8), Date, Capacity }

pub fn read_request(idm: &[u8], service: u16, start: u8, count: u8) -> Result<Vec<u8>,Error> {
    if idm.len()!=8 || count==0 || count>12 || start.checked_add(count-1).is_none() { return Err(Error::Length); }
    let mut data=vec![14+2*count,0x06]; data.extend_from_slice(idm); data.push(1);
    data.extend_from_slice(&service.to_le_bytes()); data.push(count);
    for n in start..=start+count-1 {data.extend_from_slice(&[0x80,n]);} Ok(data)
}

pub fn response_blocks<'a>(idm: &[u8], data: &'a [u8]) -> Result<&'a [u8],Error> {
    if idm.len()!=8 || data.len()<12 || data[0] as usize!=data.len() {return Err(Error::Length);}
    if data[1]!=0x07 {return Err(Error::Command);}
    if &data[2..10]!=idm {return Err(Error::Card);}
    if data[10]!=0 || data[11]!=0 {return Err(Error::Status(data[10],data[11]));}
    if data.len()<13 || data[12]==0 || data.len()!=13+16*data[12] as usize {return Err(Error::Length);}
    Ok(&data[13..])
}

pub fn attribute_balance(block: &[u8]) -> Result<u16,Error> {
    if block.len()!=16 {return Err(Error::Length)}
    Ok(u16::from_le_bytes([block[11],block[12]]))
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Record { pub raw: String, pub date: String, pub balance:u16, pub process:u8,
    pub terminal:u8, pub region:u8, pub in_line:u8, pub in_station:u8,
    pub out_line:u8, pub out_station:u8, pub sequence:u16, pub kind:&'static str }

pub fn decode_record(block:&[u8]) -> Result<Option<Record>,Error> {
    if block.len()!=16 {return Err(Error::Length)}
    let packed=u16::from_be_bytes([block[4],block[5]]);
    if packed==0 {return Ok(None)}
    let year=2000+(packed>>9);let month=(packed>>5)&15;let day=packed&31;
    let leap=year%4==0 && (year%100!=0 || year%400==0);
    let max_day=match month {1|3|5|7|8|10|12=>31,4|6|9|11=>30,2=>if leap{29}else{28},_=>0};
    if day==0 || day>max_day {return Err(Error::Date)}
    let process=block[1];
    let kind=match process {0x02|0x14|0x15|0x1f|0x23=>"topup",0x46|0x49|0x4a|0x4b|0xc6|0xcb=>"purchase",0x01|0x03|0x07|0x08|0x0d|0x0f|0x11|0x13|0x17|0x19|0x1a|0x1b|0x1c=>"transit",_=>"other"};
    let mut raw=String::with_capacity(32);for b in block {write!(&mut raw,"{b:02x}").unwrap();}
    Ok(Some(Record{raw,date:format!("{year:04}-{month:02}-{day:02}"),balance:u16::from_le_bytes([block[10],block[11]]),
        process,terminal:block[0],region:block[15],in_line:block[6],in_station:block[7],out_line:block[8],out_station:block[9],
        sequence:u16::from_be_bytes([block[13],block[14]]),kind}))
}

pub fn history_json(blocks:&[u8])->Result<String,Error> {
    if blocks.len()%16!=0 {return Err(Error::Length)}
    let mut records=Vec::new();for block in blocks.chunks_exact(16){ if let Some(r)=decode_record(block)?{records.push(r)} }
    let mut json=String::from("[");
    for (i,r) in records.iter().enumerate(){
        if i>0{json.push(',');}
        // Only a consecutive counter establishes adjacency; gaps must never become invented fares.
        let delta=records.get(i+1).filter(|p|r.sequence==p.sequence.wrapping_add(1)).map(|p|r.balance as i32-p.balance as i32);
        write!(&mut json,"{{\"raw\":\"{}\",\"date\":\"{}\",\"balance\":{},\"process\":{},\"terminal\":{},\"region\":{},\"inLine\":{},\"inStation\":{},\"outLine\":{},\"outStation\":{},\"sequence\":{},\"kind\":\"{}\",\"delta\":{}}}",r.raw,r.date,r.balance,r.process,r.terminal,r.region,r.in_line,r.in_station,r.out_line,r.out_station,r.sequence,r.kind,delta.map_or("null".into(),|n|n.to_string())).unwrap();
    } json.push(']');Ok(json)
}

unsafe fn slice<'a>(p:*const u8,n:usize)->Result<&'a[u8],Error>{if n==0{return Ok(&[])}if p.is_null()||n>65536{return Err(Error::Length)}Ok(std::slice::from_raw_parts(p,n))}
unsafe fn output(bytes:&[u8],p:*mut u8,cap:usize)->i32{if p.is_null()||bytes.len()>cap{return -1}std::ptr::copy_nonoverlapping(bytes.as_ptr(),p,bytes.len());bytes.len() as i32}

#[no_mangle]
pub unsafe extern "C" fn noca_build_read(idm:*const u8,service:u16,start:u8,count:u8,out:*mut u8,cap:usize)->i32 {
    let Ok(idm)=slice(idm,8)else{return -1};match read_request(idm,service,start,count){Ok(b)=>output(&b,out,cap),Err(_)=>-1}
}
#[no_mangle]
pub unsafe extern "C" fn noca_response(idm:*const u8,data:*const u8,len:usize,out:*mut u8,cap:usize)->i32 {
    let(Ok(idm),Ok(data))=(slice(idm,8),slice(data,len))else{return -1};match response_blocks(idm,data){Ok(b)=>output(b,out,cap),Err(Error::Status(a,b))=>-1000-(a as i32)*256-b as i32,Err(_)=>-1}
}
#[no_mangle]
pub unsafe extern "C" fn noca_history(data:*const u8,len:usize,out:*mut u8,cap:usize)->i32 {
    let Ok(data)=slice(data,len)else{return -1};match history_json(data){Ok(s)=>output(s.as_bytes(),out,cap),Err(_)=>-1}
}
#[no_mangle]
pub unsafe extern "C" fn noca_balance(data:*const u8,len:usize)->i32 {let Ok(data)=slice(data,len)else{return -1};attribute_balance(data).map_or(-1,i32::from)}

#[cfg(test)] mod tests {
use super::*;
fn block(date:u16,balance:u16,seq:u16)->[u8;16]{let mut b=[0u8;16];b[0]=0x16;b[1]=1;b[4..6].copy_from_slice(&date.to_be_bytes());b[10..12].copy_from_slice(&balance.to_le_bytes());b[13..15].copy_from_slice(&seq.to_be_bytes());b}
#[test]fn framing(){let idm=[1,2,3,4,5,6,7,8];let r=read_request(&idm,0x090f,2,2).unwrap();assert_eq!(r,vec![18,6,1,2,3,4,5,6,7,8,1,15,9,2,128,2,128,3]);assert!(read_request(&idm,0x090f,0,0).is_err());}
#[test]fn response_checks(){let idm=[1u8;8];let mut d=vec![29,7];d.extend_from_slice(&idm);d.extend_from_slice(&[0,0,1]);d.extend_from_slice(&[0u8;16]);assert_eq!(response_blocks(&idm,&d).unwrap().len(),16);assert_eq!(response_blocks(&[2u8;8],&d),Err(Error::Card));d[10]=1;assert_eq!(response_blocks(&idm,&d),Err(Error::Status(1,0)));}
#[test]fn no_truncated_response(){assert!(response_blocks(&[0u8;8],&[0u8;11]).is_err());}
#[test]fn little_endian_balance(){let mut b=[0u8;16];b[11]=0x98;b[12]=0x10;assert_eq!(attribute_balance(&b),Ok(4248));b[11]=0;b[12]=0;assert_eq!(attribute_balance(&b),Ok(0));}
#[test]fn record_fields(){let b=block((26<<9)|(10<<5)|16,4248,700);let r=decode_record(&b).unwrap().unwrap();assert_eq!(r.date,"2026-10-16");assert_eq!(r.balance,4248);assert_eq!(r.sequence,700);}
#[test]fn reject_bad_date(){assert_eq!(decode_record(&block((25<<9)|(2<<5)|29,0,1)),Err(Error::Date));assert!(decode_record(&block((24<<9)|(2<<5)|29,0,1)).is_ok());}
#[test]fn infer_only_adjacent(){let a=block((26<<9)|(10<<5)|16,4248,9);let b=block((26<<9)|(10<<5)|15,4458,8);let mut data=a.to_vec();data.extend_from_slice(&b);let j=history_json(&data).unwrap();assert!(j.contains("\"delta\":-210"));assert!(j.ends_with("\"delta\":null}]"));data[29]=0;data[30]=3;assert!(!history_json(&data).unwrap().contains("\"delta\":-210"));}
#[test]fn empty_is_not_a_transaction(){assert!(decode_record(&[0u8;16]).unwrap().is_none());assert_eq!(history_json(&[0u8;16]),Ok("[]".into()));}
}
