import React, { useState, useEffect } from 'react';
import api from './api';
import {
  Users, Building2, Calendar, Clock, DollarSign, FileText,
  ShieldCheck, Lock, Unlock, Download, Plus, CheckCircle,
  AlertCircle, LogOut, ArrowRight, UserCheck, RefreshCw, CalendarDays
} from 'lucide-react';

export default function App() {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('payroll_user');
    return saved ? JSON.parse(saved) : null;
  });

  const [activeTab, setActiveTab] = useState('dashboard');
  const [departments, setDepartments] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [attendanceList, setAttendanceList] = useState([]);
  const [overtimeList, setOvertimeList] = useState([]);
  const [payslipsList, setPayslipsList] = useState([]);
  const [rules, setRules] = useState([]);
  const [holidays, setHolidays] = useState([]);

  const [year, setYear] = useState(2026);
  const [month, setMonth] = useState(10);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState(null);
  const [successMsg, setSuccessMsg] = useState(null);

  // Form Modals state
  const [showDeptModal, setShowDeptModal] = useState(false);
  const [showEmpModal, setShowEmpModal] = useState(false);
  const [showAttModal, setShowAttModal] = useState(false);
  const [showHolidayModal, setShowHolidayModal] = useState(false);

  // Form models
  const [newDept, setNewDept] = useState({ name: '', code: '' });
  const [newEmp, setNewEmp] = useState({
    employeeCode: '', fullName: '', designation: '',
    departmentId: '', basicSalary: '', joinedDate: '2025-01-01'
  });
  const [newAtt, setNewAtt] = useState({
    employeeId: '', attendanceDate: '2026-10-05',
    checkIn: '2026-10-05T09:00', checkOut: '2026-10-05T19:00'
  });
  const [newHoliday, setNewHoliday] = useState({
    holidayDate: '2026-10-25', description: 'Deepavali Festival'
  });
  const [deductionsInput, setDeductionsInput] = useState('2000.00');

  useEffect(() => {
    if (user) {
      loadInitialData();
    }
  }, [user]);

  const showNotification = (msg, isError = false) => {
    if (isError) {
      setErrorMsg(msg);
      setTimeout(() => setErrorMsg(null), 5000);
    } else {
      setSuccessMsg(msg);
      setTimeout(() => setSuccessMsg(null), 4000);
    }
  };

  const handleLogin = async (username, password) => {
    try {
      setLoading(true);
      const res = await api.post('/auth/login', { username, password });
      const userData = res.data;
      localStorage.setItem('payroll_jwt_token', userData.token);
      localStorage.setItem('payroll_user', JSON.stringify(userData));
      setUser(userData);
      showNotification(`Welcome back, ${userData.username} (${userData.role})`);
    } catch (err) {
      showNotification(err.response?.data?.message || 'Authentication failed', true);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('payroll_jwt_token');
    localStorage.removeItem('payroll_user');
    setUser(null);
  };

  const loadInitialData = async () => {
    try {
      setLoading(true);
      const [deptRes, empRes, rulesRes, holRes] = await Promise.allSettled([
        api.get('/departments'),
        api.get('/employees?size=50'),
        api.get('/working-hour-rules'),
        api.get('/holidays')
      ]);

      if (deptRes.status === 'fulfilled') setDepartments(deptRes.value.data);
      if (empRes.status === 'fulfilled') setEmployees(empRes.value.data.content || []);
      if (rulesRes.status === 'fulfilled') setRules(rulesRes.value.data);
      if (holRes.status === 'fulfilled') setHolidays(holRes.value.data);

      loadOvertimeAndPayslips();
      loadAttendance();
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const loadAttendance = async () => {
    try {
      const res = await api.get('/attendance');
      setAttendanceList(res.data);
    } catch (err) {
      console.error(err);
    }
  };

  const loadOvertimeAndPayslips = async () => {
    try {
      const [otRes, psRes] = await Promise.allSettled([
        api.get(`/overtime?year=${year}&month=${month}`),
        api.get(`/payslips?year=${year}&month=${month}`)
      ]);
      if (otRes.status === 'fulfilled') setOvertimeList(otRes.value.data.content || []);
      if (psRes.status === 'fulfilled') setPayslipsList(psRes.value.data.content || []);
    } catch (err) {
      console.error(err);
    }
  };

  // Actions
  const handleCreateDepartment = async (e) => {
    e.preventDefault();
    try {
      const res = await api.post('/departments', newDept);
      setDepartments([...departments, res.data]);
      setShowDeptModal(false);
      setNewDept({ name: '', code: '' });
      showNotification(`Department "${res.data.name}" created successfully`);
    } catch (err) {
      showNotification(err.response?.data?.message || 'Failed to create department', true);
    }
  };

  const handleCreateEmployee = async (e) => {
    e.preventDefault();
    try {
      const res = await api.post('/employees', {
        ...newEmp,
        departmentId: Number(newEmp.departmentId),
        basicSalary: Number(newEmp.basicSalary)
      });
      setEmployees([...employees, res.data]);
      setShowEmpModal(false);
      setNewEmp({ employeeCode: '', fullName: '', designation: '', departmentId: '', basicSalary: '', joinedDate: '2025-01-01' });
      showNotification(`Employee ${res.data.fullName} registered successfully`);
    } catch (err) {
      showNotification(err.response?.data?.message || 'Failed to create employee', true);
    }
  };

  const handleRecordAttendance = async (e) => {
    e.preventDefault();
    try {
      const res = await api.post('/attendance', {
        employeeId: Number(newAtt.employeeId),
        attendanceDate: newAtt.attendanceDate,
        checkIn: newAtt.checkIn ? newAtt.checkIn + ':00' : null,
        checkOut: newAtt.checkOut ? newAtt.checkOut + ':00' : null
      });
      setAttendanceList([res.data, ...attendanceList]);
      setShowAttModal(false);
      showNotification(`Attendance logged for ${res.data.employeeName} (${res.data.dayType})`);
    } catch (err) {
      showNotification(err.response?.data?.message || 'Failed to log attendance', true);
    }
  };

  const handleCalculateOvertime = async () => {
    try {
      setLoading(true);
      const res = await api.post(`/overtime/calculate?year=${year}&month=${month}`);
      setOvertimeList(res.data);
      showNotification(`Overtime calculation completed for ${month}/${year}`);
    } catch (err) {
      showNotification(err.response?.data?.message || 'Overtime calculation failed', true);
    } finally {
      setLoading(false);
    }
  };

  const handleApproveOvertime = async (id) => {
    try {
      const res = await api.patch(`/overtime/${id}/approve`, { approvedBy: user.username });
      setOvertimeList(overtimeList.map(ot => ot.id === id ? res.data : ot));
      showNotification(`Overtime record approved by ${user.username}`);
    } catch (err) {
      showNotification(err.response?.data?.message || 'Approval failed', true);
    }
  };

  const handleGeneratePayslips = async () => {
    try {
      setLoading(true);
      const res = await api.post(`/payslips/generate?year=${year}&month=${month}&deductions=${deductionsInput}`);
      setPayslipsList(res.data);
      showNotification(`Payslips generated for ${month}/${year}`);
    } catch (err) {
      showNotification(err.response?.data?.message || 'Payslip generation failed (Ensure OT is approved)', true);
    } finally {
      setLoading(false);
    }
  };

  const handleFinalizePayslip = async (id) => {
    try {
      const res = await api.patch(`/payslips/${id}/finalize`);
      setPayslipsList(payslipsList.map(p => p.id === id ? res.data : p));
      showNotification('Payslip finalized and permanently locked');
    } catch (err) {
      showNotification(err.response?.data?.message || 'Finalization failed', true);
    }
  };

  const handleDownloadPdf = async (payslipId, employeeCode) => {
    try {
      const res = await api.get(`/payslips/${payslipId}/pdf`, { responseType: 'blob' });
      const blob = new Blob([res.data], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `payslip-${employeeCode}-${year}-${month}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      showNotification(`Downloaded payslip PDF for ${employeeCode}`);
    } catch (err) {
      showNotification('Error downloading PDF payslip', true);
    }
  };

  if (!user) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-indigo-950 flex items-center justify-center p-4">
        <div className="max-w-md w-full bg-white rounded-2xl shadow-2xl p-8 border border-slate-100">
          <div className="text-center mb-8">
            <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-indigo-600 text-white mb-4 shadow-lg shadow-indigo-500/30">
              <Building2 className="w-8 h-8" />
            </div>
            <h1 className="text-2xl font-bold text-slate-900">Provert Payroll Portal</h1>
            <p className="text-sm text-slate-500 mt-1">Enterprise Attendance & Compensation Management</p>
          </div>

          <div className="space-y-4">
            <p className="text-xs font-semibold text-slate-400 uppercase tracking-wider text-center">Quick Select Role (Demo Accounts)</p>
            
            <button
              onClick={() => handleLogin('finance_mgr', 'password123')}
              className="w-full flex items-center justify-between p-3.5 border-2 border-slate-100 hover:border-indigo-500 rounded-xl transition-all text-left group hover:bg-indigo-50/50"
            >
              <div>
                <span className="font-semibold text-slate-800 group-hover:text-indigo-600 block">Finance Manager</span>
                <span className="text-xs text-slate-500">Approve OT, generate & finalize payslips</span>
              </div>
              <ArrowRight className="w-5 h-5 text-slate-400 group-hover:text-indigo-600 transition-transform group-hover:translate-x-1" />
            </button>

            <button
              onClick={() => handleLogin('hr_admin', 'password123')}
              className="w-full flex items-center justify-between p-3.5 border-2 border-slate-100 hover:border-indigo-500 rounded-xl transition-all text-left group hover:bg-indigo-50/50"
            >
              <div>
                <span className="font-semibold text-slate-800 group-hover:text-indigo-600 block">HR Administrator</span>
                <span className="text-xs text-slate-500">Manage employees, schedules & holidays</span>
              </div>
              <ArrowRight className="w-5 h-5 text-slate-400 group-hover:text-indigo-600 transition-transform group-hover:translate-x-1" />
            </button>

            <button
              onClick={() => handleLogin('employee1', 'password123')}
              className="w-full flex items-center justify-between p-3.5 border-2 border-slate-100 hover:border-indigo-500 rounded-xl transition-all text-left group hover:bg-indigo-50/50"
            >
              <div>
                <span className="font-semibold text-slate-800 group-hover:text-indigo-600 block">Employee</span>
                <span className="text-xs text-slate-500">Log daily attendance & view payslips</span>
              </div>
              <ArrowRight className="w-5 h-5 text-slate-400 group-hover:text-indigo-600 transition-transform group-hover:translate-x-1" />
            </button>
          </div>
        </div>
      </div>
    );
  }

  // Calculate high level KPI totals
  const totalEmployees = employees.length;
  const totalDepartments = departments.length;
  const totalOtHours = (overtimeList.reduce((acc, curr) => acc + (curr.totalOtMinutes || 0), 0) / 60).toFixed(1);
  const totalPayrollNet = payslipsList.reduce((acc, curr) => acc + Number(curr.netPay || 0), 0);

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      {/* Toast Notifications */}
      {errorMsg && (
        <div className="fixed top-4 right-4 z-50 bg-rose-600 text-white px-5 py-3 rounded-xl shadow-xl flex items-center gap-3">
          <AlertCircle className="w-5 h-5" />
          <span className="text-sm font-medium">{errorMsg}</span>
        </div>
      )}
      {successMsg && (
        <div className="fixed top-4 right-4 z-50 bg-emerald-600 text-white px-5 py-3 rounded-xl shadow-xl flex items-center gap-3">
          <CheckCircle className="w-5 h-5" />
          <span className="text-sm font-medium">{successMsg}</span>
        </div>
      )}

      {/* Top Navbar */}
      <header className="bg-white border-b border-slate-200 sticky top-0 z-30">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between h-16">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-indigo-600 text-white flex items-center justify-center font-bold">
              <Building2 className="w-5 h-5" />
            </div>
            <div>
              <span className="font-bold text-slate-900 tracking-tight">PROVERT FINANCE</span>
              <span className="text-xs block text-slate-500">Payroll & Statutory Compliance Engine</span>
            </div>
          </div>

          <div className="flex items-center gap-4">
            <div className="hidden sm:flex items-center gap-2 bg-slate-100 py-1.5 px-3 rounded-lg text-xs font-medium text-slate-700">
              <ShieldCheck className="w-4 h-4 text-indigo-600" />
              <span>{user.username}</span>
              <span className="bg-indigo-100 text-indigo-700 px-2 py-0.5 rounded text-[10px] font-bold">
                {user.role.replace('ROLE_', '')}
              </span>
            </div>
            <button
              onClick={handleLogout}
              className="p-2 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-100"
              title="Sign Out"
            >
              <LogOut className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Navigation Tabs */}
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex gap-6 overflow-x-auto text-sm font-medium">
          {[
            { id: 'dashboard', label: 'Dashboard', icon: Building2 },
            { id: 'employees', label: 'Employees & Depts', icon: Users },
            { id: 'attendance', label: 'Daily Attendance', icon: Clock },
            { id: 'overtime', label: 'Overtime Engine', icon: DollarSign },
            { id: 'payslips', label: 'Payslips & PDF', icon: FileText },
            { id: 'rules', label: 'Rules & Holidays', icon: CalendarDays }
          ].map(tab => {
            const Icon = tab.icon;
            const active = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`py-3 flex items-center gap-2 border-b-2 font-medium transition-colors whitespace-nowrap ${
                  active ? 'border-indigo-600 text-indigo-600' : 'border-transparent text-slate-500 hover:text-slate-800'
                }`}
              >
                <Icon className="w-4 h-4" />
                {tab.label}
              </button>
            );
          })}
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 lg:p-8">
        
        {/* TAB 1: DASHBOARD */}
        {activeTab === 'dashboard' && (
          <div className="space-y-6">
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
              <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
                <div className="flex items-center justify-between text-slate-500">
                  <span className="text-xs font-semibold uppercase tracking-wider">Total Employees</span>
                  <Users className="w-5 h-5 text-indigo-600" />
                </div>
                <div className="text-3xl font-bold text-slate-900 mt-2">{totalEmployees}</div>
                <div className="text-xs text-slate-400 mt-1">{totalDepartments} active departments</div>
              </div>

              <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
                <div className="flex items-center justify-between text-slate-500">
                  <span className="text-xs font-semibold uppercase tracking-wider">Active Departments</span>
                  <Building2 className="w-5 h-5 text-emerald-600" />
                </div>
                <div className="text-3xl font-bold text-slate-900 mt-2">{totalDepartments}</div>
                <div className="text-xs text-slate-400 mt-1">Configured organizational units</div>
              </div>

              <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
                <div className="flex items-center justify-between text-slate-500">
                  <span className="text-xs font-semibold uppercase tracking-wider">Total OT Hours ({month}/{year})</span>
                  <Clock className="w-5 h-5 text-amber-600" />
                </div>
                <div className="text-3xl font-bold text-slate-900 mt-2">{totalOtHours} hrs</div>
                <div className="text-xs text-slate-400 mt-1">Duration-based time calculations</div>
              </div>

              <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
                <div className="flex items-center justify-between text-slate-500">
                  <span className="text-xs font-semibold uppercase tracking-wider">Net Payroll Spend</span>
                  <DollarSign className="w-5 h-5 text-emerald-600" />
                </div>
                <div className="text-3xl font-bold text-emerald-600 mt-2">Rs. {totalPayrollNet.toLocaleString()}</div>
                <div className="text-xs text-slate-400 mt-1">After EPF & statutory deductions</div>
              </div>
            </div>

            {/* Quick Actions Card */}
            <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
              <h2 className="text-base font-bold text-slate-900 mb-4">Quick Operations Workflow</h2>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <button
                  onClick={() => setShowAttModal(true)}
                  className="p-4 rounded-xl border border-slate-200 hover:border-indigo-500 hover:bg-indigo-50/30 text-left transition"
                >
                  <Clock className="w-6 h-6 text-indigo-600 mb-2" />
                  <div className="font-semibold text-slate-800">1. Log Daily Attendance</div>
                  <div className="text-xs text-slate-500 mt-1">Record check-in/out timestamps with day-type auto detection.</div>
                </button>

                <button
                  onClick={() => setActiveTab('overtime')}
                  className="p-4 rounded-xl border border-slate-200 hover:border-indigo-500 hover:bg-indigo-50/30 text-left transition"
                >
                  <DollarSign className="w-6 h-6 text-amber-600 mb-2" />
                  <div className="font-semibold text-slate-800">2. Run Monthly Overtime</div>
                  <div className="text-xs text-slate-500 mt-1">Compute OT minutes, apply multipliers and approve records.</div>
                </button>

                <button
                  onClick={() => setActiveTab('payslips')}
                  className="p-4 rounded-xl border border-slate-200 hover:border-indigo-500 hover:bg-indigo-50/30 text-left transition"
                >
                  <FileText className="w-6 h-6 text-emerald-600 mb-2" />
                  <div className="font-semibold text-slate-800">3. Generate & Finalize Payslips</div>
                  <div className="text-xs text-slate-500 mt-1">Apply 8% EPF, deductions and download branded PDF salary slips.</div>
                </button>
              </div>
            </div>
          </div>
        )}

        {/* TAB 2: EMPLOYEES & DEPARTMENTS */}
        {activeTab === 'employees' && (
          <div className="space-y-6">
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div>
                <h2 className="text-xl font-bold text-slate-900">Employee & Department Master</h2>
                <p className="text-xs text-slate-500">Manage organizational structure and employee profiles.</p>
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => setShowDeptModal(true)}
                  className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold rounded-xl flex items-center gap-1.5 transition"
                >
                  <Plus className="w-4 h-4" /> Add Department
                </button>
                <button
                  onClick={() => setShowEmpModal(true)}
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold rounded-xl flex items-center gap-1.5 transition shadow"
                >
                  <Plus className="w-4 h-4" /> Add Employee
                </button>
              </div>
            </div>

            {/* Department Badges */}
            <div className="flex flex-wrap gap-2">
              {departments.map(d => (
                <div key={d.id} className="bg-white border border-slate-200 rounded-lg px-3 py-1.5 text-xs font-medium text-slate-700 shadow-sm">
                  <span className="font-bold text-indigo-600">{d.code}</span> — {d.name}
                </div>
              ))}
            </div>

            {/* Employees Table */}
            <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
              <table className="w-full text-left border-collapse text-sm">
                <thead>
                  <tr className="bg-slate-50 text-slate-500 text-xs uppercase border-b border-slate-200">
                    <th className="py-3 px-4 font-semibold">Emp Code</th>
                    <th className="py-3 px-4 font-semibold">Full Name</th>
                    <th className="py-3 px-4 font-semibold">Department</th>
                    <th className="py-3 px-4 font-semibold">Designation</th>
                    <th className="py-3 px-4 font-semibold text-right">Basic Salary</th>
                    <th className="py-3 px-4 font-semibold text-center">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {employees.map(emp => (
                    <tr key={emp.id} className="hover:bg-slate-50/70">
                      <td className="py-3 px-4 font-bold text-indigo-600">{emp.employeeCode}</td>
                      <td className="py-3 px-4 font-medium text-slate-900">{emp.fullName}</td>
                      <td className="py-3 px-4 text-slate-600">{emp.departmentName}</td>
                      <td className="py-3 px-4 text-slate-500">{emp.designation || 'N/A'}</td>
                      <td className="py-3 px-4 text-right font-semibold text-slate-900">
                        Rs. {Number(emp.basicSalary).toLocaleString()}
                      </td>
                      <td className="py-3 px-4 text-center">
                        <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800">
                          Active
                        </span>
                      </td>
                    </tr>
                  ))}
                  {employees.length === 0 && (
                    <tr>
                      <td colSpan="6" className="text-center py-8 text-slate-400">No employees registered yet.</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* TAB 3: DAILY ATTENDANCE */}
        {activeTab === 'attendance' && (
          <div className="space-y-6">
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div>
                <h2 className="text-xl font-bold text-slate-900">Daily Attendance Logs</h2>
                <p className="text-xs text-slate-500">Track check-in and check-out with automatic Day-Type resolution.</p>
              </div>
              <button
                onClick={() => setShowAttModal(true)}
                className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold rounded-xl flex items-center gap-1.5 transition shadow"
              >
                <Plus className="w-4 h-4" /> Log Attendance
              </button>
            </div>

            <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
              <table className="w-full text-left border-collapse text-sm">
                <thead>
                  <tr className="bg-slate-50 text-slate-500 text-xs uppercase border-b border-slate-200">
                    <th className="py-3 px-4 font-semibold">Date</th>
                    <th className="py-3 px-4 font-semibold">Employee</th>
                    <th className="py-3 px-4 font-semibold text-center">Day Type</th>
                    <th className="py-3 px-4 font-semibold text-center">Check-In</th>
                    <th className="py-3 px-4 font-semibold text-center">Check-Out</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {attendanceList.map(att => (
                    <tr key={att.id} className="hover:bg-slate-50/70">
                      <td className="py-3 px-4 font-medium text-slate-900">{att.attendanceDate}</td>
                      <td className="py-3 px-4">
                        <span className="font-semibold text-slate-900">{att.employeeName}</span>
                        <span className="text-xs text-slate-400 block">{att.employeeCode}</span>
                      </td>
                      <td className="py-3 px-4 text-center">
                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                          att.dayType === 'Holiday' ? 'bg-purple-100 text-purple-800' :
                          att.dayType === 'Sunday' ? 'bg-rose-100 text-rose-800' :
                          att.dayType === 'Saturday' ? 'bg-amber-100 text-amber-800' :
                          'bg-blue-100 text-blue-800'
                        }`}>
                          {att.dayType}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-center font-mono text-xs">{att.checkIn ? att.checkIn.split('T')[1] : 'Absent'}</td>
                      <td className="py-3 px-4 text-center font-mono text-xs">{att.checkOut ? att.checkOut.split('T')[1] : 'N/A'}</td>
                    </tr>
                  ))}
                  {attendanceList.length === 0 && (
                    <tr>
                      <td colSpan="5" className="text-center py-8 text-slate-400">No attendance records logged yet.</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* TAB 4: OVERTIME ENGINE */}
        {activeTab === 'overtime' && (
          <div className="space-y-6">
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div>
                <h2 className="text-xl font-bold text-slate-900">Overtime Calculation & Managerial Approval</h2>
                <p className="text-xs text-slate-500">Duration-based OT calculation with approval-gated conflict protection.</p>
              </div>

              <div className="flex items-center gap-3">
                <select
                  value={month}
                  onChange={(e) => setMonth(Number(e.target.value))}
                  className="px-3 py-2 border border-slate-200 rounded-xl text-xs font-semibold bg-white"
                >
                  {[...Array(12).keys()].map(i => (
                    <option key={i+1} value={i+1}>Month: {i+1}</option>
                  ))}
                </select>
                <input
                  type="number"
                  value={year}
                  onChange={(e) => setYear(Number(e.target.value))}
                  className="w-20 px-3 py-2 border border-slate-200 rounded-xl text-xs font-semibold"
                />
                <button
                  onClick={handleCalculateOvertime}
                  disabled={loading}
                  className="px-4 py-2 bg-amber-600 hover:bg-amber-700 text-white text-xs font-semibold rounded-xl flex items-center gap-1.5 transition shadow"
                >
                  <RefreshCw className="w-4 h-4" /> Run Calculation
                </button>
              </div>
            </div>

            <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
              <table className="w-full text-left border-collapse text-sm">
                <thead>
                  <tr className="bg-slate-50 text-slate-500 text-xs uppercase border-b border-slate-200">
                    <th className="py-3 px-4 font-semibold">Employee</th>
                    <th className="py-3 px-4 font-semibold text-center">Period</th>
                    <th className="py-3 px-4 font-semibold text-right">OT Duration</th>
                    <th className="py-3 px-4 font-semibold text-right">Hourly Rate</th>
                    <th className="py-3 px-4 font-semibold text-right">OT Amount</th>
                    <th className="py-3 px-4 font-semibold text-center">Approval Status</th>
                    <th className="py-3 px-4 font-semibold text-center">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {overtimeList.map(ot => (
                    <tr key={ot.id} className="hover:bg-slate-50/70">
                      <td className="py-3 px-4">
                        <span className="font-semibold text-slate-900">{ot.employeeName}</span>
                        <span className="text-xs text-slate-400 block">{ot.employeeCode}</span>
                      </td>
                      <td className="py-3 px-4 text-center font-mono text-xs">{ot.month}/{ot.year}</td>
                      <td className="py-3 px-4 text-right font-semibold text-amber-600">
                        {ot.totalOtMinutes} mins ({(ot.totalOtMinutes / 60).toFixed(1)} hrs)
                      </td>
                      <td className="py-3 px-4 text-right text-slate-600">Rs. {Number(ot.hourlyRate).toFixed(2)}</td>
                      <td className="py-3 px-4 text-right font-bold text-slate-900">Rs. {Number(ot.otAmount).toLocaleString()}</td>
                      <td className="py-3 px-4 text-center">
                        {ot.approved ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800">
                            <Lock className="w-3 h-3" /> Approved by {ot.approvedBy}
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800">
                            <Unlock className="w-3 h-3" /> Pending Review
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-4 text-center">
                        {!ot.approved ? (
                          <button
                            onClick={() => handleApproveOvertime(ot.id)}
                            className="px-3 py-1 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold rounded-lg shadow transition"
                          >
                            Approve
                          </button>
                        ) : (
                          <span className="text-xs text-slate-400 italic">Locked</span>
                        )}
                      </td>
                    </tr>
                  ))}
                  {overtimeList.length === 0 && (
                    <tr>
                      <td colSpan="7" className="text-center py-8 text-slate-400">
                        No overtime records computed for this period. Click "Run Calculation" above.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* TAB 5: PAYSLIPS & PDF EXPORT */}
        {activeTab === 'payslips' && (
          <div className="space-y-6">
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div>
                <h2 className="text-xl font-bold text-slate-900">Payslip Hub & Statutory EPF/ETF</h2>
                <p className="text-xs text-slate-500">Generate finalized payslips with 8% EPF deductions and download branded PDFs.</p>
              </div>

              <div className="flex items-center gap-3">
                <input
                  type="text"
                  placeholder="Other Deductions"
                  value={deductionsInput}
                  onChange={(e) => setDeductionsInput(e.target.value)}
                  className="w-32 px-3 py-2 border border-slate-200 rounded-xl text-xs font-semibold"
                  title="Optional other voluntary deductions"
                />
                <button
                  onClick={handleGeneratePayslips}
                  disabled={loading}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold rounded-xl flex items-center gap-1.5 transition shadow"
                >
                  <RefreshCw className="w-4 h-4" /> Generate Payslips
                </button>
              </div>
            </div>

            <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
              <table className="w-full text-left border-collapse text-sm">
                <thead>
                  <tr className="bg-slate-50 text-slate-500 text-xs uppercase border-b border-slate-200">
                    <th className="py-3 px-4 font-semibold">Employee</th>
                    <th className="py-3 px-4 font-semibold text-right">Basic</th>
                    <th className="py-3 px-4 font-semibold text-right">OT Pay</th>
                    <th className="py-3 px-4 font-semibold text-right">Gross Pay</th>
                    <th className="py-3 px-4 font-semibold text-right text-rose-600">EPF (8%)</th>
                    <th className="py-3 px-4 font-semibold text-right font-bold text-emerald-700">Net Pay</th>
                    <th className="py-3 px-4 font-semibold text-center">Status</th>
                    <th className="py-3 px-4 font-semibold text-center">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {payslipsList.map(ps => (
                    <tr key={ps.id} className="hover:bg-slate-50/70">
                      <td className="py-3 px-4">
                        <span className="font-semibold text-slate-900">{ps.employeeName}</span>
                        <span className="text-xs text-slate-400 block">{ps.employeeCode}</span>
                      </td>
                      <td className="py-3 px-4 text-right font-medium text-slate-600">Rs. {Number(ps.basicSalary).toLocaleString()}</td>
                      <td className="py-3 px-4 text-right font-medium text-amber-600">Rs. {Number(ps.otAmount).toLocaleString()}</td>
                      <td className="py-3 px-4 text-right font-semibold text-slate-900">Rs. {Number(ps.grossPay).toLocaleString()}</td>
                      <td className="py-3 px-4 text-right text-rose-600 font-medium">Rs. {Number(ps.employeeEpf).toLocaleString()}</td>
                      <td className="py-3 px-4 text-right font-bold text-emerald-700 text-base">
                        Rs. {Number(ps.netPay).toLocaleString()}
                      </td>
                      <td className="py-3 px-4 text-center">
                        {ps.finalized ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800">
                            <Lock className="w-3 h-3" /> Finalized
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800">
                            <Unlock className="w-3 h-3" /> Draft
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-4 text-center">
                        <div className="flex items-center justify-center gap-2">
                          <button
                            onClick={() => handleDownloadPdf(ps.id, ps.employeeCode)}
                            className="p-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg transition"
                            title="Download PDF Salary Slip"
                          >
                            <Download className="w-4 h-4" />
                          </button>
                          {!ps.finalized && (
                            <button
                              onClick={() => handleFinalizePayslip(ps.id)}
                              className="px-2.5 py-1 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold rounded-lg shadow transition"
                              title="Permanently finalize this payslip"
                            >
                              Finalize
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                  {payslipsList.length === 0 && (
                    <tr>
                      <td colSpan="8" className="text-center py-8 text-slate-400">
                        No payslips generated for this period. Click "Generate Payslips" above.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* TAB 6: RULES & HOLIDAYS */}
        {activeTab === 'rules' && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
              <h3 className="text-base font-bold text-slate-900 mb-4">Working Hour Schedule Rules</h3>
              <div className="space-y-3">
                {rules.map(rule => (
                  <div key={rule.id} className="p-3.5 rounded-xl border border-slate-100 bg-slate-50 flex items-center justify-between">
                    <div>
                      <span className="font-bold text-slate-800">{rule.dayType}</span>
                      <span className="text-xs text-slate-500 block">
                        Expected: {rule.expectedIn} — {rule.expectedOut}
                      </span>
                    </div>
                    <div className="text-right">
                      <span className="text-xs font-bold text-indigo-600 bg-indigo-50 px-2 py-1 rounded">
                        OT: {rule.otMultiplier}x
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-base font-bold text-slate-900">Holiday Calendar</h3>
                <button
                  onClick={() => setShowHolidayModal(true)}
                  className="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold rounded-lg flex items-center gap-1"
                >
                  <Plus className="w-3.5 h-3.5" /> Add
                </button>
              </div>
              <div className="space-y-3">
                {holidays.map(hol => (
                  <div key={hol.id} className="p-3.5 rounded-xl border border-slate-100 bg-slate-50 flex items-center justify-between">
                    <div>
                      <span className="font-semibold text-slate-800 block">{hol.description || 'Public Holiday'}</span>
                      <span className="text-xs text-slate-400">{hol.holidayDate}</span>
                    </div>
                    <span className="text-xs font-semibold text-purple-700 bg-purple-100 px-2 py-0.5 rounded-full">
                      Holiday
                    </span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
      </main>

      {/* MODAL: ADD DEPARTMENT */}
      {showDeptModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 max-w-sm w-full shadow-2xl">
            <h3 className="font-bold text-slate-900 text-base mb-4">Create New Department</h3>
            <form onSubmit={handleCreateDepartment} className="space-y-3">
              <div>
                <label className="text-xs font-semibold text-slate-600 block mb-1">Department Name</label>
                <input
                  type="text"
                  required
                  value={newDept.name}
                  onChange={e => setNewDept({ ...newDept, name: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs"
                  placeholder="e.g. Operations"
                />
              </div>
              <div>
                <label className="text-xs font-semibold text-slate-600 block mb-1">Department Code</label>
                <input
                  type="text"
                  required
                  value={newDept.code}
                  onChange={e => setNewDept({ ...newDept, code: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs uppercase"
                  placeholder="e.g. OPS"
                />
              </div>
              <div className="flex gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowDeptModal(false)}
                  className="flex-1 py-2 text-xs font-semibold text-slate-600 bg-slate-100 rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="flex-1 py-2 text-xs font-semibold text-white bg-indigo-600 rounded-xl"
                >
                  Save
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: ADD EMPLOYEE */}
      {showEmpModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 max-w-md w-full shadow-2xl">
            <h3 className="font-bold text-slate-900 text-base mb-4">Register New Employee</h3>
            <form onSubmit={handleCreateEmployee} className="space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-semibold text-slate-600 block mb-1">Employee Code</label>
                  <input
                    type="text"
                    required
                    value={newEmp.employeeCode}
                    onChange={e => setNewEmp({ ...newEmp, employeeCode: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs uppercase"
                    placeholder="EMP-002"
                  />
                </div>
                <div>
                  <label className="text-xs font-semibold text-slate-600 block mb-1">Full Name</label>
                  <input
                    type="text"
                    required
                    value={newEmp.fullName}
                    onChange={e => setNewEmp({ ...newEmp, fullName: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs"
                    placeholder="Full Name"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-slate-600 block mb-1">Department</label>
                <select
                  required
                  value={newEmp.departmentId}
                  onChange={e => setNewEmp({ ...newEmp, departmentId: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs bg-white"
                >
                  <option value="">Select Department</option>
                  {departments.map(d => (
                    <option key={d.id} value={d.id}>{d.name} ({d.code})</option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-semibold text-slate-600 block mb-1">Designation</label>
                  <input
                    type="text"
                    value={newEmp.designation}
                    onChange={e => setNewEmp({ ...newEmp, designation: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs"
                    placeholder="e.g. Lead Analyst"
                  />
                </div>
                <div>
                  <label className="text-xs font-semibold text-slate-600 block mb-1">Basic Salary (LKR)</label>
                  <input
                    type="number"
                    required
                    value={newEmp.basicSalary}
                    onChange={e => setNewEmp({ ...newEmp, basicSalary: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs"
                    placeholder="120000"
                  />
                </div>
              </div>

              <div className="flex gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowEmpModal(false)}
                  className="flex-1 py-2 text-xs font-semibold text-slate-600 bg-slate-100 rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="flex-1 py-2 text-xs font-semibold text-white bg-indigo-600 rounded-xl"
                >
                  Save Employee
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: LOG ATTENDANCE */}
      {showAttModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 max-w-sm w-full shadow-2xl">
            <h3 className="font-bold text-slate-900 text-base mb-4">Log Daily Attendance</h3>
            <form onSubmit={handleRecordAttendance} className="space-y-3">
              <div>
                <label className="text-xs font-semibold text-slate-600 block mb-1">Select Employee</label>
                <select
                  required
                  value={newAtt.employeeId}
                  onChange={e => setNewAtt({ ...newAtt, employeeId: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs bg-white"
                >
                  <option value="">Select Employee</option>
                  {employees.map(e => (
                    <option key={e.id} value={e.id}>{e.fullName} ({e.employeeCode})</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="text-xs font-semibold text-slate-600 block mb-1">Attendance Date</label>
                <input
                  type="date"
                  required
                  value={newAtt.attendanceDate}
                  onChange={e => setNewAtt({ ...newAtt, attendanceDate: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-semibold text-slate-600 block mb-1">Check-In</label>
                  <input
                    type="datetime-local"
                    value={newAtt.checkIn}
                    onChange={e => setNewAtt({ ...newAtt, checkIn: e.target.value })}
                    className="w-full px-2 py-2 border border-slate-200 rounded-xl text-xs"
                  />
                </div>
                <div>
                  <label className="text-xs font-semibold text-slate-600 block mb-1">Check-Out</label>
                  <input
                    type="datetime-local"
                    value={newAtt.checkOut}
                    onChange={e => setNewAtt({ ...newAtt, checkOut: e.target.value })}
                    className="w-full px-2 py-2 border border-slate-200 rounded-xl text-xs"
                  />
                </div>
              </div>

              <div className="flex gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowAttModal(false)}
                  className="flex-1 py-2 text-xs font-semibold text-slate-600 bg-slate-100 rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="flex-1 py-2 text-xs font-semibold text-white bg-indigo-600 rounded-xl"
                >
                  Save Log
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: ADD HOLIDAY */}
      {showHolidayModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 max-w-sm w-full shadow-2xl">
            <h3 className="font-bold text-slate-900 text-base mb-4">Add Holiday</h3>
            <form onSubmit={async (e) => {
              e.preventDefault();
              try {
                const res = await api.post('/holidays', newHoliday);
                setHolidays([...holidays, res.data]);
                setShowHolidayModal(false);
                showNotification(`Holiday "${res.data.description}" added`);
              } catch (err) {
                showNotification(err.response?.data?.message || 'Failed to add holiday', true);
              }
            }} className="space-y-3">
              <div>
                <label className="text-xs font-semibold text-slate-600 block mb-1">Date</label>
                <input
                  type="date"
                  required
                  value={newHoliday.holidayDate}
                  onChange={e => setNewHoliday({ ...newHoliday, holidayDate: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs"
                />
              </div>
              <div>
                <label className="text-xs font-semibold text-slate-600 block mb-1">Description</label>
                <input
                  type="text"
                  required
                  value={newHoliday.description}
                  onChange={e => setNewHoliday({ ...newHoliday, description: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-200 rounded-xl text-xs"
                />
              </div>
              <div className="flex gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowHolidayModal(false)}
                  className="flex-1 py-2 text-xs font-semibold text-slate-600 bg-slate-100 rounded-xl"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="flex-1 py-2 text-xs font-semibold text-white bg-indigo-600 rounded-xl"
                >
                  Save Holiday
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
