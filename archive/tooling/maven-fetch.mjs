import fs from 'node:fs/promises';
import path from 'node:path';
const root='G:/Codex/2026-10-16/new-chat/work/maven';
const bases=['https://dl.google.com/dl/android/maven2/','https://repo.maven.apache.org/maven2/','https://plugins.gradle.org/m2/'];
const seen=new Map();
export const progress={modules:0,files:0,errors:[]};
const versions=new Map();
function version(s,props={}){return s?.replace(/\$\{([^}]+)\}/g,(m,k)=>props[k]??m).replace(/^\[([^,\]]+),?\]?$/,'$1')}
async function download(rel){
 const file=path.join(root,rel);try{return await fs.readFile(file)}catch{}
 for(const base of bases){try{const r=await fetch(base+rel,{signal:AbortSignal.timeout(12000)});if(!r.ok)continue;const data=new Uint8Array(await r.arrayBuffer());await fs.mkdir(path.dirname(file),{recursive:true});await fs.writeFile(file,data);progress.files++;return Buffer.from(data)}catch{}}
 return null;
}
function text(xml,tag){return xml.match(new RegExp('<'+tag+'(?:\\s[^>]*)?>([\\s\\S]*?)</'+tag+'>'))?.[1]?.trim()}
function dep(block,props){return{g:version(text(block,'groupId'),props),a:version(text(block,'artifactId'),props),v:version(text(block,'version'),props),scope:text(block,'scope'),type:text(block,'type')}}
export async function prefetch(g,a,v){
 if(!g||!a||!v||v.includes('${'))return;
 if(v.startsWith('[')||v.startsWith('(')){const meta=await download(g.replaceAll('.','/')+'/'+a+'/maven-metadata.xml');const min=v.replace(/^[[(]/,'').split(',')[0];v=min||meta?.toString().match(/<release>([^<]+)/)?.[1];if(!v)return;}
 const key=g+':'+a+':'+v;if(seen.has(key))return;seen.set(key,true);
 const dir=g.replaceAll('.','/')+'/'+a+'/'+v+'/';const stem=a+'-'+v;
 const [pomBuf,moduleBuf]=await Promise.all([download(dir+stem+'.pom'),download(dir+stem+'.module')]);
 if(!pomBuf&&!moduleBuf){progress.errors.push(key);return;}
 progress.modules++;
 let props={'project.version':v,'pom.version':v,'project.groupId':g};
 const pom=pomBuf?.toString()||'';
 const properties=text(pom,'properties')||'';for(const m of properties.matchAll(/<([\w.\-]+)>([^<]+)<\/\1>/g))props[m[1]]=m[2];
 const parent=text(pom,'parent');if(parent){const d=dep(parent,props);await prefetch(d.g,d.a,d.v)}
 const managed=text(pom,'dependencyManagement')||'';
 for(const m of managed.matchAll(/<dependency>([\s\S]*?)<\/dependency>/g)){const d=dep(m[1],props);if(d.v)versions.set(d.g+':'+d.a,d.v);if(d.scope==='import')await prefetch(d.g,d.a,d.v)}
 let json;try{json=JSON.parse(moduleBuf?.toString()||'null')}catch{}
 const dependencies=[];const files=[];
 if(json){for(const variant of json.variants||[]){const attrs=variant.attributes||{};const platform=attrs['org.jetbrains.kotlin.platform.type'];if(platform&&!['common','jvm','androidJvm'].includes(platform))continue;if(attrs['org.gradle.category']==='documentation'||/sources|javadoc/i.test(variant.name))continue;
  if(variant['available-at']){const at=variant['available-at'];dependencies.push({g:at.group,a:at.module,v:at.version})}
  for(const c of variant.dependencyConstraints||[]){const vv=c.version?.requires||c.version?.strictly||c.version?.prefers;if(vv)versions.set(c.group+':'+c.module,vv)}
  for(const d of variant.dependencies||[]){dependencies.push({g:d.group,a:d.module,v:d.version?.requires||d.version?.strictly||d.version?.prefers||versions.get(d.group+':'+d.module)})}
  for(const f of variant.files||[]){if(!/sources|javadoc/.test(f.name))files.push(f.url||f.name)}
 }}else{
  const body=pom.replace(/<dependencyManagement>[\s\S]*?<\/dependencyManagement>/g,'');
  for(const m of body.matchAll(/<dependency>([\s\S]*?)<\/dependency>/g)){const d=dep(m[1],props);if(d.scope==='test'||d.scope==='provided'||text(m[1],'optional')==='true')continue;d.v=d.v||versions.get(d.g+':'+d.a);dependencies.push(d)}
 }
 const packaging=text(pom,'packaging')||'jar';if(packaging!=='pom')files.push(stem+'.'+(packaging==='aar'?'aar':'jar'));
 for(const file of new Set(files)){if(file.includes('../'))continue;await download(dir+file)}
 for(const d of dependencies)await prefetch(d.g,d.a,d.v);
}
export async function importCache(){
 const cache='C:/Users/User/.gradle/caches/modules-2/files-2.1';let count=0;
 for(const g of await fs.readdir(cache)){for(const a of await fs.readdir(path.join(cache,g))){for(const v of await fs.readdir(path.join(cache,g,a))){const dir=path.join(cache,g,a,v);for(const h of await fs.readdir(dir)){const hashed=path.join(dir,h);let stat;try{stat=await fs.stat(hashed)}catch{continue}if(!stat.isDirectory())continue;for(const f of await fs.readdir(hashed)){if(!/\.(pom|module|jar|aar)$/.test(f))continue;const dest=path.join(root,g.replaceAll('.','/'),a,v,f);await fs.mkdir(path.dirname(dest),{recursive:true});try{await fs.access(dest)}catch{await fs.copyFile(path.join(hashed,f),dest);count++}}}}}}
 return count;
}
